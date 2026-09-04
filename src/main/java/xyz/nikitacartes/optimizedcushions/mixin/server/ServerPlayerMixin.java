package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimizedcushions.server.ServerPlayerExt;

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
    private double optimizedcushions$lastEvalX = Double.NaN;

    @Unique
    private double optimizedcushions$lastEvalZ;

    @Unique
    private long optimizedcushions$lastEvalTime;

    @Override
    public boolean optimizedcushions$skipCushionTracking(final long gameTime) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        double dx = self.getX() - this.optimizedcushions$lastEvalX;
        double dz = self.getZ() - this.optimizedcushions$lastEvalZ;
        // lastEvalX starts NaN so the first call always evaluates.
        boolean skip = dx * dx + dz * dz < RE_EVAL_DIST_SQ
                && gameTime - this.optimizedcushions$lastEvalTime < RE_EVAL_TICKS;
        if (!skip) {
            this.optimizedcushions$lastEvalX = self.getX();
            this.optimizedcushions$lastEvalZ = self.getZ();
            this.optimizedcushions$lastEvalTime = gameTime;
        }
        return skip;
    }
}
