package xyz.nikitacartes.optimizedcushions;

import net.minecraft.world.item.DyeColor;

/**
 * Implemented by each backport cushion. Baked flag is written by {@link CushionTracker};
 * colour forwards to the backport's own {@code getColor()}.
 */
public interface CushionExt {
    boolean optimizedcushions$isBaked();

    void optimizedcushions$setBaked(boolean baked);

    DyeColor optimizedcushions$color();
}
