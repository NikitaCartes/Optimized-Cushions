package xyz.nikitacartes.optimizedcushions.mixin.server;

import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.server.CushionServerTicker;
import xyz.nikitacartes.optimizedcushions.server.ServerLevelExt;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements ServerLevelExt {
    @Unique
    private CushionServerTicker optimizedcushions$cushionTicker;

    @Override
    public CushionServerTicker optimizedcushions$cushionTicker() {
        CushionServerTicker ticker = this.optimizedcushions$cushionTicker;
        if (ticker == null) {
            ticker = new CushionServerTicker((ServerLevel) (Object) this);
            this.optimizedcushions$cushionTicker = ticker;
        }
        return ticker;
    }

    // Runs our cushions in the same phase as the vanilla entity tick loop. Sits inside
    // the emptyTime < 300 branch, so an empty server pauses cushions exactly like vanilla.
    @Inject(
            method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void optimizedcushions$tickCushions(final BooleanSupplier haveTime, final CallbackInfo ci) {
        this.optimizedcushions$cushionTicker().tick();
    }
}
