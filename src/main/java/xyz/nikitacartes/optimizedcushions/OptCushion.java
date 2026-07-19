package xyz.nikitacartes.optimizedcushions;

/**
 * Marker mixed into the Cushion-Backport entity ({@code com.leclowndu93150.cushionbackport.entity.Cushion}).
 * The upstream mod (OptimizedCushions) optimises the vanilla 26.3 cushion and can reference it by type;
 * this backport optimises a third-party entity, so instead of a compile dependency we tag it with this
 * marker and test {@code entity instanceof OptCushion} wherever the upstream tested the concrete class.
 */
public interface OptCushion {
}
