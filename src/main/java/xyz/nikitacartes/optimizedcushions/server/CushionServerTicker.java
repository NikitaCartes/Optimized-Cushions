package xyz.nikitacartes.optimizedcushions.server;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.function.Consumer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import xyz.nikitacartes.optimizedcushions.mixin.server.BlockAttachedEntityAccessor;
import xyz.nikitacartes.optimizedcushions.mixin.server.ServerLevelAccessor;

/**
 * Ticks passenger-free cushions outside the vanilla entity tick list, skipping the per-entity
 * overhead (despawn checks, ticking-range lookups, profiler scopes) that dominates server time
 * on cushion-heavy maps. Cushions with passengers stay on the vanilla list so tickPassenger/
 * rideTick behave exactly as vanilla.
 *
 * <p>Every entity in this ticker is a Cushion-Backport cushion ({@link OptCushion}); it is typed
 * as {@link Entity} because this addon does not compile against the backport.
 */
public final class CushionServerTicker {
    private static final Consumer<Entity> TICK_ACTION = Entity::tick;
    private static final int CHECK_INTERVAL = 100; // BlockAttachedEntity check interval

    private final ServerLevel level;
    private final ReferenceLinkedOpenHashSet<Entity> cushions = new ReferenceLinkedOpenHashSet<>();
    // Reused snapshot so cushions removed/promoted mid-tick can't invalidate iteration.
    private Object[] iterationBuffer = new Object[0];

    public CushionServerTicker(final ServerLevel level) {
        this.level = level;
    }

    public void add(final Entity cushion) {
        if (this.cushions.add(cushion)) {
            ((CushionServerExt) cushion).optimizedcushions$setInTicker(true);
        }
    }

    public void remove(final Entity cushion) {
        if (this.cushions.remove(cushion)) {
            ((CushionServerExt) cushion).optimizedcushions$setInTicker(false);
        }
    }

    /** Hands the cushion back to the vanilla entity tick list (it got involved with passengers). */
    public void promoteToVanilla(final Entity cushion) {
        this.remove(cushion);
        ((ServerLevelAccessor) this.level).optimizedcushions$getEntityTickList().add(cushion);
    }

    /** Reclaims a vanilla-ticked cushion once it is passenger-free again. */
    public void demoteIfIdle(final Entity cushion) {
        CushionServerExt ext = (CushionServerExt) cushion;
        if (ext.optimizedcushions$isServerTicking() && !ext.optimizedcushions$isInTicker()
                && !cushion.isPassenger() && cushion.getPassengers().isEmpty()) {
            ((ServerLevelAccessor) this.level).optimizedcushions$getEntityTickList().remove(cushion);
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
        // non-player entity without player passengers. Tick-freezing (the /tick command)
        // only exists from 1.20.3, so there is nothing to honour before that.
        //? if >=1.20.3 {
        if (!this.level.tickRateManager().runsNormally()) {
            return;
        }
        //?}

        if (this.iterationBuffer.length < count) {
            this.iterationBuffer = new Object[count + (count >> 1)];
        }
        Object[] buffer = this.cushions.toArray(this.iterationBuffer);
        DistanceManager distanceManager = this.level.getChunkSource().chunkMap.getDistanceManager();

        for (int i = 0; i < count; i++) {
            Entity cushion = (Entity) buffer[i];
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
            // ChunkPos#pack was named toLong before the 26.1 rename snapshot.
            //? if >=26.1 {
            long chunkKey = cushion.chunkPosition().pack();
            //?} else
            /*long chunkKey = cushion.chunkPosition().toLong();*/
            if (accessor.optimizedcushions$getTicksSinceLastCheck() >= CHECK_INTERVAL
                    && !distanceManager.inEntityTickingRange(chunkKey)) {
                // vanilla freezes the whole tick outside entity-ticking range; we
                // instead defer the survives/fluid poll by another interval. Amortizes the
                // ticking-range lookup to once per 100 ticks per cushion.
                accessor.optimizedcushions$setTicksSinceLastCheck(0);
                continue;
            }
            cushion.setOldPosAndRot();
            cushion.tickCount++;
            this.level.guardEntityTick(TICK_ACTION, cushion);
        }
    }
}
