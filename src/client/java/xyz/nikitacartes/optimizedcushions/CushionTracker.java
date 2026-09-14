package xyz.nikitacartes.optimizedcushions;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * Client-side index of cushion entities, grouped by the chunk section that owns them
 * (the section of {@code blockPosition()}). Event-driven: load/unload events and the
 * setPos/onSyncedDataUpdated mixin hooks feed a dirty queue drained at end of tick.
 * Mutated only on the client main thread; read from section meshing worker threads
 * via the concurrent {@link #BY_SECTION} map holding immutable snapshots.
 *
 * <p>The baked flag is deliberately not flipped here. Section remeshing completes
 * asynchronously, so the flag flips through {@link CushionSectionTasks} at the moment
 * the rebuilt mesh is installed: no invisible frame when baking, no double render
 * when unbaking.
 */
public final class CushionTracker {
    /** Immutable per-cushion state used for baking. */
    public record Snapshot(Cushion cushion, double x, double y, double z, Direction dir, DyeColor color, BlockPos lightPos, long sectionKey, boolean bakeable) {
    }

    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Set<Cushion> DIRTY = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Snapshot>> BY_SECTION = new ConcurrentHashMap<>();
    private static Level lastLevel;

    private CushionTracker() {
    }

    public static void onLoad(final Cushion cushion) {
        // Flushing only in tick() would wipe spawn-chunk cushions: on a level change
        // their ENTITY_LOAD fires before the first end-of-tick.
        if (cushion.level() != lastLevel) {
            flush(cushion.level());
        }
        update(cushion);
    }

    public static void onUnload(final Cushion cushion) {
        DIRTY.remove(cushion);
        drop(cushion);
    }

    public static void markChanged(final Cushion cushion) {
        DIRTY.add(cushion);
    }

    public static void tick(final Minecraft minecraft) {
        if (minecraft.level != lastLevel) {
            flush(minecraft.level);
        }

        if (DIRTY.isEmpty()) {
            return;
        }

        for (Cushion cushion : DIRTY) {
            if (cushion.isRemoved() || cushion.level() != minecraft.level) {
                drop(cushion);
            } else {
                update(cushion);
            }
        }
        DIRTY.clear();
    }

    /** Whether the given cushion is currently rendered as chunk geometry instead of an entity model. */
    public static boolean isBaked(final Cushion cushion) {
        return ((CushionExt)cushion).optimizedcushions$isBaked();
    }

    /** Read from section meshing threads. Returns null when the section has no baked cushions. */
    public static Map<Integer, Snapshot> getForSection(final long sectionKey) {
        return BY_SECTION.get(sectionKey);
    }

    private static void flush(final Level level) {
        SNAPSHOTS.clear();
        BY_SECTION.clear();
        DIRTY.clear();
        CushionSectionTasks.clear();
        lastLevel = level;
    }

    private static void update(final Cushion cushion) {
        Snapshot next = snapshot(cushion);
        Snapshot prev = SNAPSHOTS.put(cushion.getId(), next);
        if (next.equals(prev)) {
            return;
        }

        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
            if (!next.bakeable()) {
                // The copy stays in the installed mesh until this section rebuilds;
                // go back to the entity path only then, never while it is still shown.
                CushionSectionTasks.addTask(prev.sectionKey(), () -> commitUnbaked(cushion.getId(), prev.sectionKey(), cushion));
            }
        }

        if (next.bakeable()) {
            BY_SECTION.computeIfAbsent(next.sectionKey(), key -> new ConcurrentHashMap<>()).put(cushion.getId(), next);
            if (prev == null || !prev.bakeable() || prev.sectionKey() != next.sectionKey()) {
                markDirty(next.sectionKey());
            }
            // No flag flip here: the section compiler queues commitBakedSection, which
            // runs when the rebuilt mesh is installed, so the entity is never missing.
        }
    }

    private static void drop(final Cushion cushion) {
        Snapshot prev = SNAPSHOTS.remove(cushion.getId());
        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
            CushionSectionTasks.addTask(prev.sectionKey(), () -> commitUnbaked(cushion.getId(), prev.sectionKey(), cushion));
        } else {
            ((CushionExt)cushion).optimizedcushions$setBaked(false);
        }
    }

    // isCurrentlyGlowing, not Minecraft.shouldEntityAppearGlowing: its extra branch
    // (spectator outlines) only applies to players.
    private static boolean isBakeable(final Cushion cushion) {
        return !cushion.isCurrentlyGlowing() && !cushion.displayFireAnimation() && !cushion.isInvisible();
    }

    /**
     * Runs through {@link CushionSectionTasks} when a rebuilt section mesh is installed:
     * every cushion baked into that mesh stops rendering as an entity at exactly the
     * moment its mesh copy appears. Reads only the concurrent section map and writes
     * only the volatile baked flag, so meshing worker threads may run this.
     */
    public static void commitBakedSection(final long sectionKey) {
        Map<Integer, Snapshot> section = BY_SECTION.get(sectionKey);
        if (section == null) {
            return;
        }
        for (Snapshot snapshot : section.values()) {
            ((CushionExt)snapshot.cushion()).optimizedcushions$setBaked(true);
        }
    }

    /**
     * Runs through {@link CushionSectionTasks} when a rebuilt section mesh is installed:
     * a cushion whose copy is no longer in that mesh goes back to the entity path at
     * exactly the moment the new mesh appears. The identity check keeps a stale task
     * from touching an unrelated cushion that reused the entity id.
     */
    public static void commitUnbaked(final int id, final long sectionKey, final Cushion cushion) {
        Map<Integer, Snapshot> section = BY_SECTION.get(sectionKey);
        Snapshot current = section == null ? null : section.get(id);
        if (current == null || current.cushion() != cushion) {
            ((CushionExt)cushion).optimizedcushions$setBaked(false);
        }
    }

    private static Snapshot snapshot(final Cushion cushion) {
        BlockPos lightPos = BlockPos.containing(cushion.getLightProbePosition(1.0F));
        return new Snapshot(
            cushion,
            cushion.getX(),
            cushion.getY(),
            cushion.getZ(),
            Direction.fromYRot(cushion.getYRot()),
            cushion.getColor(),
            lightPos,
            SectionPos.asLong(cushion.blockPosition()),
            isBakeable(cushion)
        );
    }

    private static void removeFromSection(final long sectionKey, final int id) {
        ConcurrentHashMap<Integer, Snapshot> section = BY_SECTION.get(sectionKey);
        if (section != null) {
            section.remove(id);
            if (section.isEmpty()) {
                BY_SECTION.remove(sectionKey, section);
            }
        }
    }

    private static void markDirty(final long sectionKey) {
        Minecraft.getInstance().levelExtractor.setSectionDirty(SectionPos.x(sectionKey), SectionPos.y(sectionKey), SectionPos.z(sectionKey));
    }
}
