package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionTracker;
import xyz.nikitacartes.optimizedcushions.OptCushion;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void optimizedcushions$onDataUpdated(final EntityDataAccessor<?> accessor, final CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof OptCushion && self.level() != null && self.level().isClientSide()) {
            CushionTracker.markChanged(self);
        }
    }
}
