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
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.server.ServerPlayerExt;
import xyz.nikitacartes.optimizedcushions.server.TrackedEntityExt;

/**
 * Two tracker optimisations for static cushions. Plain fields: ChunkMap is single-threaded.
 *
 * <p>Injection robustness (OC-05): both {@code tick()} wraps are kept intentionally narrow:
 * {@code lastSectionPos GETFIELD} + {@code inEntityTickingRange INVOKE} are the smallest
 * stable sites inside the per-entity loop. Alternatives like wrapping
 * {@code ServerEntity.sendChanges()} would be more stable but would lose the
 * ticking-range gate. The current handshake via
 * {@code optimizedcushions$currentEntityQuiescent} assumes the vanilla
 * {@code if (sectionPosChanged || needsSync || inEntityTickingRange)} ordering. A
 * {@code Slice} from {@code SectionPos.of(Entity)} to {@code ServerEntity.sendChanges()}
 * bounds both wraps to that loop so an extra condition inserted before the loop does not
 * shift them, and {@code tick HEAD} resets the flag so a short-circuited
 * {@code needsSync} path never leaks staleness. If Mojang reorders or inlines
 * {@code lastSectionPos}, the worst case is a single extra {@code sendChanges()} call,
 * not a crash; named targets without {@code ordinal} are used everywhere.</p>
 * <p>OC-05 hardening: HEAD reset guarantees no stale quiescent leaks; skip path falls back
 * to vanilla ({@code !quiescent && original.call}) so a missed classification merely costs
 * one extra sendChanges, never hides an update. Defensive copy of the quiescent check uses
 * only fields that Mojang cannot remove without breaking the tracking contract (syncPosition,
 * syncVelocity, isDirty, passengers, lastPassengers).</p>
 * <p>OC-15: quiescent requires empty syncPosition/syncVelocity/isDirty/passengers/lastPassengers.
 * A dye change via SynchedEntityData isDirty==true so not quiescent; a GameEvent/POI side-effect
 * is not gated but cushions do not trigger those. Passenger lag (lastPassengers one tick behind)
 * can delay a color/name packet by one ChunkMap.tick cycle, but rider's own tracker still sends.</p>
 */
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

    // OC-43: move() also has a fast-path `updatePlayers(List)` for the moving player's
    // own tracker and for tick's moved-players batch. Without throttling that path the
    // 1-block/20-tick grid is bypassed for high player counts.
    @WrapOperation(
            method = "move(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;updatePlayers(Ljava/util/List;)V"
            )
    )
    private void optimizedcushions$skipStaticUpdatePlayers(
            final @Coerce Object trackedEntity, final java.util.List<ServerPlayer> players, final Operation<Void> original
    ) {
        if (this.optimizedcushions$skipCushionsThisMove
                && ((TrackedEntityExt) trackedEntity).optimizedcushions$entity() instanceof Cushion) {
            return;
        }
        original.call(trackedEntity, players);
    }

    // tick(): for a quiescent cushion sendChanges() is a guaranteed no-op, so skip it and
    // the ticking-range lookup. Anything that would make it send flips a checked flag first.
    // GETFIELD slice is intentionally absent: lastSectionPos is read before SectionPos.of,
    // so a slice from SectionPos.of would exclude it. Named target without ordinal is kept.
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
        // OC-05 hardening: defensive classification — any exception or unexpected type
        // falls back to vanilla (quiescent=false) so the cushion still ticks/tracks.
        try {
            TrackedEntityExt ext = (TrackedEntityExt) trackedEntity;
            Entity entity = ext.optimizedcushions$entity();
            // needsSync isn't listed: when set, the || chain short-circuits before inEntityTickingRange,
            // but GETFIELD still runs before the if, so classification is not stale. HEAD reset
            // above guarantees no leak if vanilla ever moves the field read after the if.
            this.optimizedcushions$currentEntityQuiescent = entity instanceof Cushion
                    && !entity.syncVelocity
                    && !entity.syncPosition
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
        // If classification did not run (vanilla reorder), quiescent is false -> vanilla path, safe fallback.
        // Guard the distance check too — any exception falls back to vanilla.
        try {
            if (this.optimizedcushions$currentEntityQuiescent) {
                return false;
            }
        } catch (Exception ignored) {
            // fall through to vanilla
        }
        return original.call(distanceManager, chunkKey);
    }
}
