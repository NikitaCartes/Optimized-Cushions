package xyz.nikitacartes.optimizedcushions.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Cushion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.server.ServerPlayerExt;
import xyz.nikitacartes.optimizedcushions.server.TrackedEntityExt;

/**
 * Tracker optimisations for static cushions. Plain fields: ChunkMap is single-threaded.
 * The tick() fast-path lives in ServerEntityMixin so VMP (which overwrites ChunkMap.tick())
 * stays compatible.
 */
@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Unique
    private boolean optimizedcushions$skipCushionsThisMove;

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

    @WrapOperation(
            method = "addEntity(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;clientTrackingRange()I"
            )
    )
    private int optimizedcushions$widenCushionRange(final EntityType<?> type, final Operation<Integer> original) {
        // Vanilla tracks cushions only 10 chunks (160 blocks), so the client never receives
        // (and cannot bake) cushions past that. Cushions are static, so widen to 32 chunks and
        // let updatePlayer's min(range, viewDistance) cap it at chunk view distance.
        if (type == EntityTypes.CUSHION) {
            return 32;
        }
        return original.call(type);
    }
}
