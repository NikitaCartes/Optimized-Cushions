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
    private static final Set<Cushion> DIRTY = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Cushion> TRACKED = Collections.newSetFromMap(new IdentityHashMap<>());
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
        TRACKED.remove(cushion);
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

        // Snapshot to avoid CME and lost-update when markChanged is re-entered during drain (OC-32/OC-41/OC-47).
        // Use removeAll(snapshot) instead of clear() so entries added between snapshot and removal are kept (OC-41).
        List<Cushion> pending = new ArrayList<>(DIRTY);
        DIRTY.removeAll(pending);
        for (Cushion cushion : pending) {
            if (cushion.isRemoved() || cushion.level() != minecraft.level) {
                drop(cushion);
            } else {
                update(cushion);
            }
        }
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
        // Clear baked flags on survivors (OC-24): entities that survive level swap but have no BY_SECTION entry.
        for (Cushion cushion : TRACKED) {
            ((CushionExt) cushion).optimizedcushions$setBaked(false);
        }
        TRACKED.clear();
        SNAPSHOTS.clear();
        // Clear inner maps before outer to avoid ghost meshes on workers holding inner reference (OC-34).
        for (ConcurrentHashMap<Integer, Snapshot> inner : BY_SECTION.values()) {
            inner.clear();
        }
        BY_SECTION.clear();
        DIRTY.clear();
        lastLevel = level;
        // Invalidate cached model after level change (pack reload may have swapped models) (OC-38).
        // CushionBaker invalidation is handled via model identity + root check; no forced null needed
        // here, but TRACKED clearing above ensures stale baked flags do not leak.
    }

    private static void update(final Cushion cushion) {
        Snapshot next = snapshot(cushion);
        Snapshot prev = SNAPSHOTS.put(cushion.getId(), next);
        ((CushionExt)cushion).optimizedcushions$setBaked(next.bakeable());
        TRACKED.add(cushion);
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
        } else {
            // If cushion became non-bakeable but previously tracked, ensure removal already handled above.
            // If it was bakeable and now not, we already removed. No extra work.
        }
    }

    private static void drop(final Cushion cushion) {
        ((CushionExt)cushion).optimizedcushions$setBaked(false);
        TRACKED.remove(cushion);
        Snapshot prev = SNAPSHOTS.remove(cushion.getId());
        if (prev != null && prev.bakeable()) {
            removeFromSection(prev.sectionKey(), cushion.getId());
            markDirty(prev.sectionKey());
        }
    }

    // isCurrentlyGlowing, not Minecraft.shouldEntityAppearGlowing: its extra branch
    // (spectator outlines) only applies to players.
    // OC-13 (intentional): named cushions remain bakeable (they get chunk geometry plus
    // an entity name-plate). LevelExtractorMixin culls non-named baked cushions; the
    // CushionRendererMixin keeps submitNameDisplay so hasCustomName cushions pay one extra
    // CushionRenderState allocation per frame but still save the model draw. The Snapshot
    // bakeable flag does not encode hasCustomName — culling is handled separately.
    private static boolean isBakeable(final Cushion cushion) {
        return !cushion.isCurrentlyGlowing() && !cushion.displayFireAnimation() && !cushion.isInvisible();
    }

    private static Snapshot snapshot(final Cushion cushion) {
        // OC-39: probe (0.5,0.85,0.5 above base) can fall in a different section than
        // blockPosition when the cushion sits at a section boundary (x/z %16==15 or y%16==0
        // on a slab). RenderSectionRegion is only a 3x3 cube around the compiled section;
        // region.getBrightness(lightPos) outside that cube returns 0 -> baked full-dark.
        // Sampling light at blockPosition keeps lightPos inside the same section as sectionKey
        // and matches block-like lighting. Probe-based sampling would need a fallback clamp
        // or neighbourhood 3x3 lookup; blockPosition is the simpler stable fix.
        BlockPos lightPos = cushion.blockPosition();
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelExtractor == null) {
            return;
        }
        try {
            mc.levelExtractor.setSectionDirty(SectionPos.x(sectionKey), SectionPos.y(sectionKey), SectionPos.z(sectionKey));
        } catch (Exception ignored) {
        }
    }
}
