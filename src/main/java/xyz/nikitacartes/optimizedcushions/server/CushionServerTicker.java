package xyz.nikitacartes.optimizedcushions.server;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.function.Consumer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import xyz.nikitacartes.optimizedcushions.mixin.server.ServerLevelAccessor;

/**
 * Ticks passenger-free cushions outside the vanilla entity tick list, skipping the per-entity
 * overhead (despawn checks, ticking-range lookups, profiler scopes) that dominates server time
 * on cushion-heavy maps. Cushions with passengers stay on the vanilla list so tickPassenger/
 * rideTick behave exactly as vanilla.
 */
public final class CushionServerTicker {
    private static final Consumer<Entity> TICK_ACTION = Entity::tick;

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
        if (ext.optimizedcushions$isInTicker()) {
            return;
        }
        if (cushion.isRemoved()) {
            return;
        }
        if (cushion.isPassenger() || !cushion.getPassengers().isEmpty()) {
            return;
        }
        ((ServerLevelAccessor) this.level).optimizedcushions$getEntityTickList().remove(cushion);
        this.add(cushion);
    }

    /** Runs right after the vanilla entity tick loop, same game-tick phase. */
    public void tick() {
        int count = this.cushions.size();
        if (count == 0) {
            return;
        }
        // Dual guard with the injection site: the call sits inside the
        // emptyTime < 300 branch, but re-check here so a vanilla restructure
        // cannot silently leave cushions ticking on an empty server.
        if (((ServerLevelAccessor) this.level).optimizedcushions$getEmptyTime() >= 300) {
            return;
        }
        //? if >=1.20.3 {
        // Global fast-path; the per-entity isEntityFrozen check below stays
        // authoritative for selective freeze.
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
            //? if >=1.20.3 {
            if (this.level.tickRateManager().isEntityFrozen(cushion)) {
                continue;
            }
            //?}
            //? if >=26.1 {
            if (!distanceManager.inEntityTickingRange(cushion.chunkPosition().pack())) {
                continue;
            }
            //?} else {
            /*if (!distanceManager.inEntityTickingRange(cushion.chunkPosition().toLong())) {
                continue;
            }
            *///?}
            // Entity.commonTick() inlined: it exists only on 26.3, where its body is exactly
            // these statements (plus a client-side interpolation no-op on the server).
            cushion.setOldPosAndRot();
            cushion.tickCount++;
            if (cushion.invulnerableTime > 0) {
                cushion.invulnerableTime--;
            }
            this.level.guardEntityTick(TICK_ACTION, cushion);
        }
    }
}
