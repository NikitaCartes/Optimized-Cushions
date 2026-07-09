package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.world.entity.decoration.Cushion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionExt;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(Cushion.class)
public class CushionMixin implements CushionExt {
    @Unique
    private boolean optimizedcushions$baked;

    @Override
    public boolean optimizedcushions$isBaked() {
        return this.optimizedcushions$baked;
    }

    @Override
    public void optimizedcushions$setBaked(final boolean baked) {
        this.optimizedcushions$baked = baked;
    }

    // Block-attached entities never interpolate: fires on spawn/teleport only.
    // Rotation-only teleports are covered too, moveTo always calls setPos.
    @Inject(method = "setPos(DDD)V", at = @At("TAIL"))
    private void optimizedcushions$onSetPos(final double x, final double y, final double z, final CallbackInfo ci) {
        Cushion self = (Cushion)(Object)this;
        if (self.level() != null && self.level().isClientSide()) {
            CushionTracker.markChanged(self);
        }
    }
}
