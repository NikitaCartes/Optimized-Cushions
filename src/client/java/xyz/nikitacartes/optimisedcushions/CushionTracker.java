package xyz.nikitacartes.optimisedcushions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.DyeColor;

/**
 * Client-side index of cushion entities, grouped by the chunk section that owns them
 * (the section of {@code blockPosition()}). Mutated only on the client main thread
 * (entity load/unload events + end-of-tick scan); read from section meshing worker
 * threads via the concurrent {@link #BY_SECTION} map holding immutable snapshots.
 */
public final class CushionTracker {
    /** Immutable per-cushion state used for baking. */
    public record Snapshot(double x, double y, double z, Direction dir, DyeColor color, BlockPos lightPos, long sectionKey, boolean bakeable) {
    }

    private static final Map<Integer, Cushion> TRACKED = new HashMap<>();
    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Snapshot>> BY_SECTION = new ConcurrentHashMap<>();
    private static ClientLevel lastLevel;

    private CushionTracker() {
    }

    public static void onLoad(final Cushion cushion) {
        TRACKED.put(cushion.getId(), cushion);
        update(cushion);
    }

    public static void onUnload(final Cushion cushion) {
        TRACKED.remove(cushion.getId());
        drop(cushion.getId());
    }

    public static void tick(final Minecraft minecraft) {
        if (minecraft.level != lastLevel) {
            clearAll();
            lastLevel = minecraft.level;
        }

        if (TRACKED.isEmpty()) {
            return;
        }

        Iterator<Cushion> iterator = TRACKED.values().iterator();
        while (iterator.hasNext()) {
            Cushion cushion = iterator.next();
            if (cushion.isRemoved() || cushion.level() != minecraft.level) {
                iterator.remove();
                drop(cushion.getId());
            } else {
                update(cushion);
            }
        }
    }

    /** Whether the given cushion is currently rendered as chunk geometry instead of an entity model. */
    public static boolean isBaked(final Cushion cushion) {
        Snapshot snapshot = SNAPSHOTS.get(cushion.getId());
        return snapshot != null && snapshot.bakeable();
    }

    /** Read from section meshing threads. Returns null when the section has no baked cushions. */
    public static Map<Integer, Snapshot> getForSection(final long sectionKey) {
        return BY_SECTION.get(sectionKey);
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
        }

        if (next.bakeable()) {
            BY_SECTION.computeIfAbsent(next.sectionKey(), key -> new ConcurrentHashMap<>()).put(cushion.getId(), next);
            if (prev == null || !prev.bakeable() || prev.sectionKey() != next.sectionKey()) {
                markDirty(next.sectionKey());
            }
        }
    }

    private static void drop(final int id) {
        Snapshot prev = SNAPSHOTS.remove(id);
        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), id);
            markDirty(prev.sectionKey());
        }
    }

    private static void clearAll() {
        TRACKED.clear();
        SNAPSHOTS.clear();
        BY_SECTION.clear();
    }

    private static Snapshot snapshot(final Cushion cushion) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean bakeable = !minecraft.shouldEntityAppearGlowing(cushion) && !cushion.displayFireAnimation() && !cushion.isInvisible();
        BlockPos lightPos = BlockPos.containing(cushion.getLightProbePosition(1.0F));
        return new Snapshot(
            cushion.getX(),
            cushion.getY(),
            cushion.getZ(),
            Direction.fromYRot(cushion.getYRot()),
            cushion.getColor(),
            lightPos,
            SectionPos.asLong(cushion.blockPosition()),
            bakeable
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
