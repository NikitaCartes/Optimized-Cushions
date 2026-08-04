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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * Client-side index of Cushion-Backport cushions, grouped by the chunk section of
 * {@code blockPosition()}. Load/unload events and the setPos/onSyncedDataUpdated hooks feed a dirty
 * queue drained at end of tick. Mutated on the client main thread only; read from section meshing
 * threads through {@link #BY_SECTION}, which holds immutable snapshots. Cushions are typed as
 * {@link Entity} and their colour read through the {@link CushionExt} duck: no compile dependency.
 */
public final class CushionTracker {
    /** Immutable per-cushion state used for baking. */
    public record Snapshot(double x, double y, double z, Direction dir, DyeColor color, BlockPos lightPos, long sectionKey, boolean bakeable) {
    }

    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Set<Entity> DIRTY = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Snapshot>> BY_SECTION = new ConcurrentHashMap<>();
    private static Level lastLevel;

    private CushionTracker() {
    }

    public static void onLoad(final Entity cushion) {
        // Flushing only in tick() would wipe spawn-chunk cushions: on a level change
        // their ENTITY_LOAD fires before the first end-of-tick.
        if (cushion.level() != lastLevel) {
            flush(cushion.level());
        }
        update(cushion);
    }

    public static void onUnload(final Entity cushion) {
        DIRTY.remove(cushion);
        drop(cushion);
    }

    public static void markChanged(final Entity cushion) {
        DIRTY.add(cushion);
    }

    public static void tick(final Minecraft minecraft) {
        if (minecraft.level != lastLevel) {
            flush(minecraft.level);
        }

        if (DIRTY.isEmpty()) {
            return;
        }

        for (Entity cushion : DIRTY) {
            if (cushion.isRemoved() || cushion.level() != minecraft.level) {
                drop(cushion);
            } else {
                update(cushion);
            }
        }
        DIRTY.clear();
    }

    /** Whether the given cushion is currently rendered as chunk geometry instead of an entity model. */
    public static boolean isBaked(final Entity cushion) {
        return ((CushionExt) cushion).optimizedcushions$isBaked();
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

    private static void update(final Entity cushion) {
        Snapshot next = snapshot(cushion);
        Snapshot prev = SNAPSHOTS.put(cushion.getId(), next);
        ((CushionExt) cushion).optimizedcushions$setBaked(next.bakeable());
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

    private static void drop(final Entity cushion) {
        ((CushionExt) cushion).optimizedcushions$setBaked(false);
        Snapshot prev = SNAPSHOTS.remove(cushion.getId());
        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
        }
    }

    // isCurrentlyGlowing, not Minecraft.shouldEntityAppearGlowing: its extra branch
    // (spectator outlines) only applies to players.
    private static boolean isBakeable(final Entity cushion) {
        return !cushion.isCurrentlyGlowing() && !cushion.displayFireAnimation() && !cushion.isInvisible();
    }

    private static Snapshot snapshot(final Entity cushion) {
        BlockPos lightPos = BlockPos.containing(cushion.getLightProbePosition(1.0F));
        return new Snapshot(
            cushion.getX(),
            cushion.getY(),
            cushion.getZ(),
            Direction.fromYRot(cushion.getYRot()),
            ((CushionExt) cushion).optimizedcushions$color(),
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
        int x = SectionPos.x(sectionKey);
        int y = SectionPos.y(sectionKey);
        int z = SectionPos.z(sectionKey);
        //? if >=26.2 {
        Minecraft.getInstance().levelExtractor.setSectionDirty(x, y, z);
        //?} else
        /*Minecraft.getInstance().levelRenderer.setSectionDirty(x, y, z);*/
    }
}
