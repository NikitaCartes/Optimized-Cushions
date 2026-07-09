package xyz.nikitacartes.optimisedcushions.server;

/** Duck interface on {@code ServerPlayer}: throttles cushion tracker re-evaluation. */
public interface ServerPlayerExt {
    /**
     * True when cushion visibility for this player was evaluated recently enough
     * (moved less than a block and less than a second ago) to skip this pass.
     * When it returns false the evaluation is recorded as happening now.
     */
    boolean optimisedcushions$skipCushionTracking(long gameTime);
}
