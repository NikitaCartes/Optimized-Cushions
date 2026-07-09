package xyz.nikitacartes.optimisedcushions.server;

/** Duck interface on {@code Cushion}: where the server side currently ticks it. */
public interface CushionServerExt {
    /** True between onTickingStart and onTickingEnd on the server. */
    boolean optimisedcushions$isServerTicking();

    void optimisedcushions$setServerTicking(boolean ticking);

    /** True while owned by {@link CushionServerTicker} instead of the vanilla entity tick list. */
    boolean optimisedcushions$isInTicker();

    void optimisedcushions$setInTicker(boolean inTicker);
}
