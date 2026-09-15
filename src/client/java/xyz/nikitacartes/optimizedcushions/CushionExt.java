package xyz.nikitacartes.optimizedcushions;

/** Implemented by {@code Cushion} via mixin; the flag flips via {@link CushionSectionTasks} when the rebuilt mesh is installed and is read from the render thread. */
public interface CushionExt {
    boolean optimizedcushions$isBaked();

    void optimizedcushions$setBaked(boolean baked);
}
