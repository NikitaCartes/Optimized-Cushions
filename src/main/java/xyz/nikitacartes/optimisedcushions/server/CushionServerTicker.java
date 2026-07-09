package xyz.nikitacartes.optimisedcushions.server;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.function.Consumer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;
import xyz.nikitacartes.optimisedcushions.mixin.server.BlockAttachedEntityAccessor;
import xyz.nikitacartes.optimisedcushions.mixin.server.ServerLevelAccessor;

/**
 * Ticks passenger-free cushions outside the vanilla entity tick list, skipping the per-entity
 * overhead (despawn checks, ticking-range lookups, profiler scopes) that dominates server time
 * on cushion-heavy maps. Cushions with passengers stay on the vanilla list so tickPassenger/
 * rideTick behave exactly as vanilla.
 */
public final class CushionServerTicker {
    private static final Consumer<Cushion> TICK_ACTION = Entity::tick;
    private static final int CHECK_INTERVAL = 100; // BlockAttachedEntity.CHECK_INTERVAL

    private final ServerLevel level;
    private final ReferenceLinkedOpenHashSet<Cushion> cushions = new ReferenceLinkedOpenHashSet<>();
    // Reused snapshot so cushions removed/promoted mid-tick can't invalidate iteration.
    private Object[] iterationBuffer = new Object[0];

    public CushionServerTicker(final ServerLevel level) {
        this.level = level;
    }

    public void add(final Cushion cushion) {
        if (this.cushions.add(cushion)) {
            ((CushionServerExt) cushion).optimisedcushions$setInTicker(true);
        }
    }

    public void remove(final Cushion cushion) {
        if (this.cushions.remove(cushion)) {
            ((CushionServerExt) cushion).optimisedcushions$setInTicker(false);
        }
    }

    /** Hands the cushion back to the vanilla entity tick list (it got involved with passengers). */
    public void promoteToVanilla(final Cushion cushion) {
        this.remove(cushion);
        ((ServerLevelAccessor) this.level).optimisedcushions$getEntityTickList().add(cushion);
    }

    /** Reclaims a vanilla-ticked cushion once it is passenger-free again. */
    public void demoteIfIdle(final Cushion cushion) {
        CushionServerExt ext = (CushionServerExt) cushion;
        if (ext.optimisedcushions$isServerTicking() && !ext.optimisedcushions$isInTicker()
                && !cushion.isPassenger() && cushion.getPassengers().isEmpty()) {
            ((ServerLevelAccessor) this.level).optimisedcushions$getEntityTickList().remove(cushion);
            this.add(cushion);
        }
    }

    /** Runs right after the vanilla entity tick loop, same game-tick phase. */
    public void tick() {
        int count = this.cushions.size();
        if (count == 0) {
            return;
        }
        // Same result as vanilla's per-entity isEntityFrozen: everything here is a
        // non-player entity without player passengers.
        if (!this.level.tickRateManager().runsNormally()) {
            return;
        }

        if (this.iterationBuffer.length < count) {
            this.iterationBuffer = new Object[count + (count >> 1)];
        }
        Object[] buffer = this.cushions.toArray(this.iterationBuffer);
        DistanceManager distanceManager = this.level.getChunkSource().chunkMap.getDistanceManager();

        for (int i = 0; i < count; i++) {
            Cushion cushion = (Cushion) buffer[i];
            buffer[i] = null;
            if (cushion.isRemoved()) {
                this.remove(cushion);
                continue;
            }
            // Safety net; the Entity passenger hooks normally promote eagerly.
            if (!cushion.getPassengers().isEmpty() || cushion.isPassenger()) {
                this.promoteToVanilla(cushion);
                continue;
            }
            BlockAttachedEntityAccessor accessor = (BlockAttachedEntityAccessor) cushion;
            if (accessor.optimisedcushions$getTicksSinceLastCheck() >= CHECK_INTERVAL
                    && !distanceManager.inEntityTickingRange(cushion.chunkPosition().pack())) {
                // vanilla freezes the whole tick outside entity-ticking range; we
                // instead defer the survives/fluid poll by another interval. Amortizes the
                // ticking-range lookup to once per 100 ticks per cushion.
                accessor.optimisedcushions$setTicksSinceLastCheck(0);
                continue;
            }
            cushion.setOldPosAndRot();
            cushion.tickCount++;
            this.level.guardEntityTick(TICK_ACTION, cushion);
        }
    }
}
