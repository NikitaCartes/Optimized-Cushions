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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * Client-side cushion index by owning section. Only ClientLevel cushions are tracked;
 * schematic Levels rebuild on worker threads. The baked flag flips on mesh install
 * via {@link CushionSectionTasks}, never here.
 */
public final class CushionTracker {
    /** Immutable bake state. */
    public record Snapshot(Cushion cushion, double x, double y, double z, Direction dir, DyeColor color, BlockPos lightPos, long sectionKey, boolean bakeable) {
    }

    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();
    /** Identity set; synchronized because schematic rebuilds call markChanged off-thread. */
    private static final Set<Cushion> DIRTY = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Snapshot>> BY_SECTION = new ConcurrentHashMap<>();
    private static Level lastLevel;

    private CushionTracker() {
    }

    public static void onLoad(final Cushion cushion) {
        // Schematic Levels are client-side but not ClientLevel; ignore, often off-thread.
        if (!(cushion.level() instanceof ClientLevel)) {
            return;
        }
        // ENTITY_LOAD fires before the first tick on level change; flush here.
        if (cushion.level() != lastLevel) {
            flush(cushion.level());
        }
        update(cushion);
    }

    public static void onUnload(final Cushion cushion) {
        if (!(cushion.level() instanceof ClientLevel)) {
            return;
        }
        DIRTY.remove(cushion);
        drop(cushion);
    }

    public static void markChanged(final Cushion cushion) {
        if (!(cushion.level() instanceof ClientLevel)) {
            return;
        }
        DIRTY.add(cushion);
    }

    public static void tick(final Minecraft minecraft) {
        if (minecraft.level != lastLevel) {
            flush(minecraft.level);
        }

        final List<Cushion> pending;
        // Drain under lock; adds during the drain land on the next tick.
        synchronized (DIRTY) {
            if (DIRTY.isEmpty()) {
                return;
            }
            pending = new ArrayList<>(DIRTY);
            pending.forEach(DIRTY::remove);
        }
        for (Cushion cushion : pending) {
            if (cushion.isRemoved() || cushion.level() != minecraft.level) {
                drop(cushion);
            } else {
                update(cushion);
            }
        }
    }

    /** True when rendered as chunk geometry instead of an entity model. */
    public static boolean isBaked(final Cushion cushion) {
        return ((CushionExt)cushion).optimizedcushions$isBaked();
    }

    /** Worker-thread read; null when the section holds no baked cushions. */
    public static Map<Integer, Snapshot> getForSection(final long sectionKey) {
        return BY_SECTION.get(sectionKey);
    }

    private static void flush(final Level level) {
        // Level-swap survivors keep identity; unbake via snapshots so none stays culled.
        for (Snapshot snapshot : SNAPSHOTS.values()) {
            ((CushionExt) snapshot.cushion()).optimizedcushions$setBaked(false);
        }
        SNAPSHOTS.clear();
        // Inners first: a worker holding an inner ref must not bake stale data into a reused key.
        for (ConcurrentHashMap<Integer, Snapshot> inner : BY_SECTION.values()) {
            inner.clear();
        }
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
                // Entity path resumes only after the mesh without the copy installs.
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

    // Not shouldEntityAppearGlowing: its spectator branch is player-only.
    private static boolean isBakeable(final Cushion cushion) {
        return !cushion.isCurrentlyGlowing() && !cushion.displayFireAnimation() && !cushion.isInvisible();
    }

    /** Runs on mesh install; map reads plus volatile flag writes only. */
    public static void commitBakedSection(final long sectionKey) {
        Map<Integer, Snapshot> section = BY_SECTION.get(sectionKey);
        if (section == null) {
            return;
        }
        for (Snapshot snapshot : new ArrayList<>(section.values())) {
            ((CushionExt)snapshot.cushion()).optimizedcushions$setBaked(true);
        }
    }

    /** Runs on mesh install; the identity check blocks stale tasks on reused ids. */
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        try {
            mc.levelExtractor.setSectionDirty(SectionPos.x(sectionKey), SectionPos.y(sectionKey), SectionPos.z(sectionKey));
        } catch (Exception ignored) {
        }
    }
}
