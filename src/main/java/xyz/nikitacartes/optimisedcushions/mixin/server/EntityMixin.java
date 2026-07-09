package xyz.nikitacartes.optimisedcushions.mixin.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimisedcushions.server.CushionServerExt;
import xyz.nikitacartes.optimisedcushions.server.ServerLevelExt;

/**
 * Moves cushions between our ticker and the vanilla tick list the moment passengers are
 * involved, so tickPassenger/rideTick run vanilla-exact on the very tick they should.
 * Covers both a cushion being sat on and a cushion made to ride something (/ride).
 */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "addPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void optimisedcushions$onAddPassenger(final Entity passenger, final CallbackInfo ci) {
        optimisedcushions$promote((Entity) (Object) this);
        optimisedcushions$promote(passenger);
    }

    @Inject(method = "removePassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void optimisedcushions$onRemovePassenger(final Entity passenger, final CallbackInfo ci) {
        optimisedcushions$demote((Entity) (Object) this);
        optimisedcushions$demote(passenger);
    }

    @Unique
    private static void optimisedcushions$promote(final Entity entity) {
        if (entity instanceof Cushion cushion && cushion.level() instanceof ServerLevel level
                && ((CushionServerExt) cushion).optimisedcushions$isInTicker()) {
            ((ServerLevelExt) level).optimisedcushions$cushionTicker().promoteToVanilla(cushion);
        }
    }

    @Unique
    private static void optimisedcushions$demote(final Entity entity) {
        if (entity instanceof Cushion cushion && cushion.level() instanceof ServerLevel level) {
            ((ServerLevelExt) level).optimisedcushions$cushionTicker().demoteIfIdle(cushion);
        }
    }
}
