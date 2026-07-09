package xyz.nikitacartes.optimisedcushions.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimisedcushions.server.ServerPlayerExt;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ServerPlayerExt {
    // 1 block of horizontal movement against a >=32 block spawn/despawn radius; the
    // 20-tick cap matches the idle movement-packet interval, so newly sent chunks get
    // their cushions evaluated within a second even for a stationary player.
    @Unique
    private static final double RE_EVAL_DIST_SQ = 1.0;

    @Unique
    private static final long RE_EVAL_TICKS = 20L;

    @Unique
    private double optimisedcushions$lastEvalX = Double.NaN;

    @Unique
    private double optimisedcushions$lastEvalZ;

    @Unique
    private long optimisedcushions$lastEvalTime;

    @Override
    public boolean optimisedcushions$skipCushionTracking(final long gameTime) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        double dx = self.getX() - this.optimisedcushions$lastEvalX;
        double dz = self.getZ() - this.optimisedcushions$lastEvalZ;
        // lastEvalX starts NaN so the first call always evaluates.
        boolean skip = dx * dx + dz * dz < RE_EVAL_DIST_SQ
                && gameTime - this.optimisedcushions$lastEvalTime < RE_EVAL_TICKS;
        if (!skip) {
            this.optimisedcushions$lastEvalX = self.getX();
            this.optimisedcushions$lastEvalZ = self.getZ();
            this.optimisedcushions$lastEvalTime = gameTime;
        }
        return skip;
    }
}
