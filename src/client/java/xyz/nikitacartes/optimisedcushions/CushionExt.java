package xyz.nikitacartes.optimisedcushions;

/** Implemented by {@code Cushion} via mixin; the flag is written by {@link CushionTracker} on the main thread only. */
public interface CushionExt {
    boolean optimisedcushions$isBaked();

    void optimisedcushions$setBaked(boolean baked);
}
