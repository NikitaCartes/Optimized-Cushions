package xyz.nikitacartes.optimisedcushions.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimisedcushions.server.ServerPlayerExt;
import xyz.nikitacartes.optimisedcushions.server.TrackedEntityExt;

/**
 * Two tracker optimizations for static cushions. Both fields are safe as plain instance
 * state: ChunkMap runs on the server thread and neither move() nor tick() is reentrant.
 */
@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Unique
    private boolean optimisedcushions$skipCushionsThisMove;

    @Unique
    private boolean optimisedcushions$currentEntityQuiescent;

    // --- move(): vanilla re-evaluates visibility of EVERY tracked entity on every
    // movement packet (rotation-only included). Cushions never move, so their visibility
    // for a player only changes with player movement; a sub-block move changes nothing.

    @Inject(method = "move(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void optimisedcushions$classifyMove(final ServerPlayer player, final CallbackInfo ci) {
        this.optimisedcushions$skipCushionsThisMove =
                ((ServerPlayerExt) player).optimisedcushions$skipCushionTracking(player.level().getGameTime());
    }

    @WrapOperation(
            method = "move(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
            )
    )
    private void optimisedcushions$skipStaticUpdatePlayer(
            final @Coerce Object trackedEntity, final ServerPlayer player, final Operation<Void> original
    ) {
        if (this.optimisedcushions$skipCushionsThisMove
                && ((TrackedEntityExt) trackedEntity).optimisedcushions$entity() instanceof Cushion) {
            return;
        }
        original.call(trackedEntity, player);
    }

    // --- tick(): vanilla does a ticking-range hash lookup plus sendChanges() for every
    // tracked entity every tick. For a quiescent cushion sendChanges() is a guaranteed
    // no-op, so both are skipped. Any state that would make it send something (teleport,
    // dye, glowing, hurt, mount/dismount) flips one of the checked flags first.

    @WrapOperation(
            method = "tick()V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;lastSectionPos:Lnet/minecraft/core/SectionPos;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private SectionPos optimisedcushions$classifyTrackedEntity(
            final @Coerce Object trackedEntity, final Operation<SectionPos> original
    ) {
        TrackedEntityExt ext = (TrackedEntityExt) trackedEntity;
        Entity entity = ext.optimisedcushions$entity();
        // needsSync is not checked here: when it is set, the || chain short-circuits
        // before the ticking-range call and vanilla handles the entity in full.
        this.optimisedcushions$currentEntityQuiescent = entity instanceof Cushion
                && !entity.syncVelocity
                && !entity.syncPosition
                && !entity.getEntityData().isDirty()
                && entity.getPassengers().isEmpty()
                && ((ServerEntityAccessor) ext.optimisedcushions$serverEntity())
                        .optimisedcushions$getLastPassengers().isEmpty();
        return original.call(trackedEntity);
    }

    @WrapOperation(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap$DistanceManager;inEntityTickingRange(J)Z"
            )
    )
    private boolean optimisedcushions$skipQuiescentSendChanges(
            final @Coerce Object distanceManager, final long chunkKey, final Operation<Boolean> original
    ) {
        return !this.optimisedcushions$currentEntityQuiescent && original.call(distanceManager, chunkKey);
    }
}
