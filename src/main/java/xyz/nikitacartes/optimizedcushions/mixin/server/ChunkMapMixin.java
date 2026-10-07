package xyz.nikitacartes.optimizedcushions.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.OptCushion;
import xyz.nikitacartes.optimizedcushions.server.ServerPlayerExt;
import xyz.nikitacartes.optimizedcushions.server.TrackedEntityExt;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Unique
    private boolean optimizedcushions$skipCushionsThisMove;

    @Unique
    private boolean optimizedcushions$currentEntityQuiescent;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void optimizedcushions$resetQuiescent(final CallbackInfo ci) {
        this.optimizedcushions$currentEntityQuiescent = false;
    }

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
                && ((TrackedEntityExt) trackedEntity).optimizedcushions$entity() instanceof OptCushion) {
            return;
        }
        original.call(trackedEntity, player);
    }

    @WrapOperation(
            method = "addEntity(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;clientTrackingRange()I"
            )
    )
    private int optimizedcushions$widenCushionRange(final EntityType<?> type, final Operation<Integer> original) {
        // No compile dependency on Cushion-Backport: match its entity by registry id.
        // Cushions are static, so widen to 32 chunks and let updatePlayer's
        // min(range, viewDistance) cap it at chunk view distance.
        if ("cushionbackport:cushion".equals(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type)))) {
            return 32;
        }
        return original.call(type);
    }

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
        try {
            TrackedEntityExt ext = (TrackedEntityExt) trackedEntity;
            Entity entity = ext.optimizedcushions$entity();
            this.optimizedcushions$currentEntityQuiescent = entity instanceof OptCushion
                    && !entity.hurtMarked
                    //? if >=26.2 {
                    && !entity.syncPosition
                    //?}
                    && !entity.getEntityData().isDirty()
                    && entity.getPassengers().isEmpty()
                    && ((ServerEntityAccessor) ext.optimizedcushions$serverEntity())
                            .optimizedcushions$getLastPassengers().isEmpty();
        } catch (Exception e) {
            this.optimizedcushions$currentEntityQuiescent = false;
        }
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
        if (this.optimizedcushions$currentEntityQuiescent) {
            return false;
        }
        return original.call(distanceManager, chunkKey);
    }
}
