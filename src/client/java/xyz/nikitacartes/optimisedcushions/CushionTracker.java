package xyz.nikitacartes.optimisedcushions;

import java.util.HashMap;
import java.util.HashSet;
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
 */
public final class CushionTracker {
    /** Immutable per-cushion state used for baking. */
    public record Snapshot(double x, double y, double z, Direction dir, DyeColor color, BlockPos lightPos, long sectionKey, boolean bakeable) {
    }

    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Set<Cushion> DIRTY = new HashSet<>();
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
        return ((CushionExt)cushion).optimisedcushions$isBaked();
    }

    /** Read from section meshing threads. Returns null when the section has no baked cushions. */
    public static Map<Integer, Snapshot> getForSection(final long sectionKey) {
        return BY_SECTION.get(sectionKey);
    }

    private static void flush(final Level level) {
        SNAPSHOTS.clear();
        BY_SECTION.clear();
        DIRTY.clear();
        lastLevel = level;
    }

    private static void update(final Cushion cushion) {
        Snapshot next = snapshot(cushion);
        Snapshot prev = SNAPSHOTS.put(cushion.getId(), next);
        ((CushionExt)cushion).optimisedcushions$setBaked(next.bakeable());
        if (next.equals(prev)) {
            return;
        }

        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
        }

        if (next.bakeable()) {
            BY_SECTION.computeIfAbsent(next.sectionKey(), key -> new ConcurrentHashMap<>()).put(cushion.getId(), next);
            if (prev == null || !prev.bakeable() || prev.sectionKey() != next.sectionKey()) {
                markDirty(next.sectionKey());
            }
        }
    }

    private static void drop(final Cushion cushion) {
        ((CushionExt)cushion).optimisedcushions$setBaked(false);
        Snapshot prev = SNAPSHOTS.remove(cushion.getId());
        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
        }
    }

    // isCurrentlyGlowing, not Minecraft.shouldEntityAppearGlowing: its extra branch
    // (spectator outlines) only applies to players.
    private static boolean isBakeable(final Cushion cushion) {
        return !cushion.isCurrentlyGlowing() && !cushion.displayFireAnimation() && !cushion.isInvisible();
    }

    private static Snapshot snapshot(final Cushion cushion) {
        BlockPos lightPos = BlockPos.containing(cushion.getLightProbePosition(1.0F));
        return new Snapshot(
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
