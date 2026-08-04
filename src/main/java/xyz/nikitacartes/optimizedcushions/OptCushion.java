package xyz.nikitacartes.optimizedcushions;

/**
 * Marker mixed into the Cushion-Backport entity ({@code com.leclowndu93150.cushionbackport.entity.Cushion}).
 * This addon has no compile dependency on that mod, so every place where upstream tested the concrete
 * cushion type tests {@code entity instanceof OptCushion} instead.
 */
public interface OptCushion {
}
