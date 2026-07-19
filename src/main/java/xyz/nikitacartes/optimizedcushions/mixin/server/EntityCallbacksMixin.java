package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.OptCushion;
import xyz.nikitacartes.optimizedcushions.server.CushionServerExt;
import xyz.nikitacartes.optimizedcushions.server.ServerLevelExt;

/** Routes cushions into {@code CushionServerTicker} instead of the vanilla entity tick list. */
@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public class EntityCallbacksMixin {
    @Inject(method = "onTickingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void optimizedcushions$routeTickingStart(final Entity entity, final CallbackInfo ci) {
        if (!(entity instanceof OptCushion)) {
            return;
        }
        ((CushionServerExt) entity).optimizedcushions$setServerTicking(true);
        // Passenger mechanics (tickPassenger/rideTick) need the vanilla tick list.
        if (entity.isPassenger() || !entity.getPassengers().isEmpty()) {
            return;
        }
        ((ServerLevelExt) entity.level()).optimizedcushions$cushionTicker().add(entity);
        ci.cancel();
    }

    @Inject(method = "onTickingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void optimizedcushions$routeTickingEnd(final Entity entity, final CallbackInfo ci) {
        if (!(entity instanceof OptCushion)) {
            return;
        }
        ((CushionServerExt) entity).optimizedcushions$setServerTicking(false);
        ((ServerLevelExt) entity.level()).optimizedcushions$cushionTicker().remove(entity);
        // The vanilla entityTickList.remove still runs; it is a no-op when we owned the cushion.
    }
}
