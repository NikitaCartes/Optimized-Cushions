package xyz.nikitacartes.optimizedcushions.mixin.server;

import java.util.List;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.server.CushionServerExt;

/**
 * Skips sendChanges() for quiescent cushions at the ServerEntity level so the fast-path
 * survives ChunkMap.tick() overwrites (VMP replaces tick() via NearbyEntityTracking,
 * which still calls ServerEntity.sendChanges()).
 */
@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @Shadow
    private List<Entity> lastPassengers;

    @Inject(method = "sendChanges()V", at = @At("HEAD"), cancellable = true)
    private void optimizedcushions$skipQuiescentCushion(final CallbackInfo ci) {
        Entity self = this.entity;
        if (self instanceof CushionServerExt
                && !self.hurtMarked
                //? if >=26.2 {
                && !self.syncPosition
                //?}
                //? if >=1.21.11 {
                && !self.needsSync
                //?}
                && !self.getEntityData().isDirty()
                && self.getPassengers().isEmpty()
                && this.lastPassengers.isEmpty()) {
            ci.cancel();
        }
    }
}
