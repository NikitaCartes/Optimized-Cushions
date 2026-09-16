package xyz.nikitacartes.optimizedcushions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
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

public final class CushionTracker {
    public record Snapshot(Entity cushion, double x, double y, double z, Direction dir, DyeColor color, BlockPos lightPos, long sectionKey, boolean bakeable) {
    }

    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Set<Entity> DIRTY = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Snapshot>> BY_SECTION = new ConcurrentHashMap<>();
    private static Level lastLevel;

    private CushionTracker() {
    }

    public static void onLoad(final Entity cushion) {
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

        List<Entity> pending = new ArrayList<>(DIRTY);
        pending.forEach(DIRTY::remove);
        for (Entity cushion : pending) {
            if (cushion.isRemoved() || cushion.level() != minecraft.level) {
                drop(cushion);
            } else {
                update(cushion);
            }
        }
    }

    public static boolean isBaked(final Entity cushion) {
        return ((CushionExt) cushion).optimizedcushions$isBaked();
    }

    public static Map<Integer, Snapshot> getForSection(final long sectionKey) {
        return BY_SECTION.get(sectionKey);
    }

    private static void flush(final Level level) {
        for (Snapshot snapshot : SNAPSHOTS.values()) {
            ((CushionExt) snapshot.cushion()).optimizedcushions$setBaked(false);
        }
        SNAPSHOTS.clear();
        for (ConcurrentHashMap<Integer, Snapshot> inner : BY_SECTION.values()) {
            inner.clear();
        }
        BY_SECTION.clear();
        DIRTY.clear();
        CushionSectionTasks.clear();
        lastLevel = level;
    }

    private static void update(final Entity cushion) {
        Snapshot next = snapshot(cushion);
        Snapshot prev = SNAPSHOTS.put(cushion.getId(), next);
        if (next.equals(prev)) {
            return;
        }

        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
            if (!next.bakeable()) {
                CushionSectionTasks.addTask(prev.sectionKey(), () -> commitUnbaked(cushion.getId(), prev.sectionKey(), cushion));
            }
        }

        if (next.bakeable()) {
            BY_SECTION.computeIfAbsent(next.sectionKey(), key -> new ConcurrentHashMap<>()).put(cushion.getId(), next);
            if (prev == null || !prev.bakeable() || prev.sectionKey() != next.sectionKey()) {
                markDirty(next.sectionKey());
            }
        }
    }

    private static void drop(final Entity cushion) {
        Snapshot prev = SNAPSHOTS.remove(cushion.getId());
        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
            CushionSectionTasks.addTask(prev.sectionKey(), () -> commitUnbaked(cushion.getId(), prev.sectionKey(), cushion));
        } else {
            ((CushionExt) cushion).optimizedcushions$setBaked(false);
        }
    }

    private static boolean isBakeable(final Entity cushion) {
        return !cushion.isCurrentlyGlowing() && !cushion.displayFireAnimation() && !cushion.isInvisible();
    }

    public static void commitBakedSection(final long sectionKey) {
        Map<Integer, Snapshot> section = BY_SECTION.get(sectionKey);
        if (section == null) {
            return;
        }
        for (Snapshot snapshot : new ArrayList<>(section.values())) {
            ((CushionExt) snapshot.cushion()).optimizedcushions$setBaked(true);
        }
    }

    public static void commitUnbaked(final int id, final long sectionKey, final Entity cushion) {
        Map<Integer, Snapshot> section = BY_SECTION.get(sectionKey);
        Snapshot current = section == null ? null : section.get(id);
        if (current == null || current.cushion() != cushion) {
            ((CushionExt) cushion).optimizedcushions$setBaked(false);
        }
    }

    private static Snapshot snapshot(final Entity cushion) {
        BlockPos lightPos = BlockPos.containing(cushion.getLightProbePosition(1.0F));
        return new Snapshot(
            cushion,
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int x = SectionPos.x(sectionKey);
        int y = SectionPos.y(sectionKey);
        int z = SectionPos.z(sectionKey);
        try {
            //? if >=26.2 {
            mc.levelExtractor.setSectionDirty(x, y, z);
            //?} else
            /*mc.levelRenderer.setSectionDirty(x, y, z);*/
        } catch (Exception ignored) {
        }
    }
}
