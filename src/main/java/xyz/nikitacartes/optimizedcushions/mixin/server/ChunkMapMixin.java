package xyz.nikitacartes.optimizedcushions.mixin.server;

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
import xyz.nikitacartes.optimizedcushions.server.ServerPlayerExt;
import xyz.nikitacartes.optimizedcushions.server.TrackedEntityExt;

/** Two tracker optimisations for static cushions. Plain fields: ChunkMap is single-threaded. */
@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Unique
    private boolean optimizedcushions$skipCushionsThisMove;

    @Unique
    private boolean optimizedcushions$currentEntityQuiescent;

    // move(): cushions never move, so their visibility only changes when the player does;
    // vanilla re-checks every tracked entity on every move packet, even rotation-only ones.
    @Inject(method = "move(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void optimizedcushions$classifyMove(final ServerPlayer player, final CallbackInfo ci) {
        this.optimizedcushions$skipCushionsThisMove =
                ((ServerPlayerExt) player).optimizedcushions$skipCushionTracking(player.level().getGameTime());
    }

    @WrapOperation(
            method = "move(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
            )
    )
    private void optimizedcushions$skipStaticUpdatePlayer(
            final @Coerce Object trackedEntity, final ServerPlayer player, final Operation<Void> original
    ) {
        if (this.optimizedcushions$skipCushionsThisMove
                && ((TrackedEntityExt) trackedEntity).optimizedcushions$entity() instanceof Cushion) {
            return;
        }
        original.call(trackedEntity, player);
    }

    // tick(): for a quiescent cushion sendChanges() is a guaranteed no-op, so skip it and
    // the ticking-range lookup. Anything that would make it send flips a checked flag first.
    @WrapOperation(
            method = "tick()V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;lastSectionPos:Lnet/minecraft/core/SectionPos;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private SectionPos optimizedcushions$classifyTrackedEntity(
            final @Coerce Object trackedEntity, final Operation<SectionPos> original
    ) {
        TrackedEntityExt ext = (TrackedEntityExt) trackedEntity;
        Entity entity = ext.optimizedcushions$entity();
        // needsSync isn't listed: when set, the || chain short-circuits before this runs.
        this.optimizedcushions$currentEntityQuiescent = entity instanceof Cushion
                && !entity.syncVelocity
                && !entity.syncPosition
                && !entity.getEntityData().isDirty()
                && entity.getPassengers().isEmpty()
                && ((ServerEntityAccessor) ext.optimizedcushions$serverEntity())
                        .optimizedcushions$getLastPassengers().isEmpty();
        return original.call(trackedEntity);
    }

    @WrapOperation(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap$DistanceManager;inEntityTickingRange(J)Z"
            )
    )
    private boolean optimizedcushions$skipQuiescentSendChanges(
            final @Coerce Object distanceManager, final long chunkKey, final Operation<Boolean> original
    ) {
        return !this.optimizedcushions$currentEntityQuiescent && original.call(distanceManager, chunkKey);
    }
}
