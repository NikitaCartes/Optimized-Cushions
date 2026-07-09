package xyz.nikitacartes.optimizedcushions.server;

/** Duck interface on {@code Cushion}: where the server side currently ticks it. */
public interface CushionServerExt {
    /** True between onTickingStart and onTickingEnd on the server. */
    boolean optimizedcushions$isServerTicking();

    void optimizedcushions$setServerTicking(boolean ticking);

    /** True while owned by {@link CushionServerTicker} instead of the vanilla entity tick list. */
    boolean optimizedcushions$isInTicker();

    void optimizedcushions$setInTicker(boolean inTicker);
}
