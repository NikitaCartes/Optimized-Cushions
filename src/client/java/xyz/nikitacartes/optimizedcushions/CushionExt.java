package xyz.nikitacartes.optimizedcushions;

import net.minecraft.world.item.DyeColor;

/**
 * Implemented by the Cushion-Backport entity via a client mixin. The baked flag is written by
 * {@link CushionTracker} on the main thread only; the colour accessor forwards to the backport's
 * own {@code getColor()} (shadowed) so the tracker can snapshot it without a compile dependency.
 */
public interface CushionExt {
    boolean optimizedcushions$isBaked();

    void optimizedcushions$setBaked(boolean baked);

    DyeColor optimizedcushions$color();
}
