package xyz.nikitacartes.optimizedcushions;

/** Implemented by {@code Cushion} via mixin; the flag is written by {@link CushionTracker} on the main thread only. */
public interface CushionExt {
    boolean optimizedcushions$isBaked();

    void optimizedcushions$setBaked(boolean baked);
}
