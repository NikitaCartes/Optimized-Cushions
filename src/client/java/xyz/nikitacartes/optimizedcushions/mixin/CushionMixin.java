package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionExt;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(targets = "com.leclowndu93150.cushionbackport.entity.Cushion")
public abstract class CushionMixin implements CushionExt {
    // The backport Cushion's own colour accessor; shadowed so the tracker can read it.
    @Shadow
    public abstract DyeColor getColor();

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

    @Override
    public DyeColor optimizedcushions$color() {
        return this.getColor();
    }

    // Block-attached entities never interpolate: fires on spawn/teleport only.
    // Rotation-only teleports are covered too, moveTo always calls setPos.
    @Inject(method = "setPos(DDD)V", at = @At("TAIL"))
    private void optimizedcushions$onSetPos(final double x, final double y, final double z, final CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() != null && self.level().isClientSide()) {
            CushionTracker.markChanged(self);
        }
    }
}
