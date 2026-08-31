package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimizedcushions.server.ServerPlayerExt;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ServerPlayerExt {
    // OC-11: intentional hard-coded trade-off (no config file today). 1.0 block sq (now 3D)
    // against a >=32 block tracking radius gives ~3% false-negative spawn lag at the far edge;
    // 20 ticks matches the idle movement-packet interval so newly sent chunks get cushions
    // evaluated within a second even for a stationary player. Server ops can tune by editing
    // these two constants and rebuilding; a config file would add I/O on every move packet
    // for a niche farm setting. Documented in README trade-offs.
    @Unique
    private static final double RE_EVAL_DIST_SQ = 1.0;

    @Unique
    private static final long RE_EVAL_TICKS = 20L;

    @Unique
    private double optimizedcushions$lastEvalX = Double.NaN;

    @Unique
    private double optimizedcushions$lastEvalZ;

    @Unique
    private double optimizedcushions$lastEvalY = Double.NaN;

    @Unique
    private long optimizedcushions$lastEvalTime;

    @Override
    public boolean optimizedcushions$skipCushionTracking(final long gameTime) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        double dx = self.getX() - this.optimizedcushions$lastEvalX;
        double dy = self.getY() - this.optimizedcushions$lastEvalY;
        double dz = self.getZ() - this.optimizedcushions$lastEvalZ;
        // lastEvalX starts NaN so the first call always evaluates.
        boolean skip = dx * dx + dy * dy + dz * dz < RE_EVAL_DIST_SQ
                && gameTime - this.optimizedcushions$lastEvalTime < RE_EVAL_TICKS;
        if (!skip) {
            this.optimizedcushions$lastEvalX = self.getX();
            this.optimizedcushions$lastEvalY = self.getY();
            this.optimizedcushions$lastEvalZ = self.getZ();
            this.optimizedcushions$lastEvalTime = gameTime;
        }
        return skip;
    }
}
