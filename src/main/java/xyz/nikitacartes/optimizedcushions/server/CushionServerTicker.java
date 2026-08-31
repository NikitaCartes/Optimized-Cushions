package xyz.nikitacartes.optimizedcushions.server;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;
import xyz.nikitacartes.optimizedcushions.mixin.server.ServerLevelAccessor;

public final class CushionServerTicker {
    private static final Consumer<Cushion> TICK_ACTION = Entity::tick;

    private final ServerLevel level;
    private final ReferenceLinkedOpenHashSet<Cushion> cushions = new ReferenceLinkedOpenHashSet<>();
    private final ReferenceLinkedOpenHashSet<Cushion> pendingDemote = new ReferenceLinkedOpenHashSet<>();
    private Object[] iterationBuffer = new Object[0];

    public CushionServerTicker(final ServerLevel level) {
        this.level = level;
    }

    public void add(final Cushion cushion) {
        if (this.cushions.add(cushion)) {
            ((CushionServerExt) cushion).optimizedcushions$setInTicker(true);
            pendingDemote.remove(cushion);
        }
    }

    public void remove(final Cushion cushion) {
        if (this.cushions.remove(cushion)) {
            ((CushionServerExt) cushion).optimizedcushions$setInTicker(false);
        }
        pendingDemote.remove(cushion);
    }

    public void promoteToVanilla(final Cushion cushion) {
        this.remove(cushion);
        pendingDemote.remove(cushion);
        // OC-23: route through PersistentEntitySectionManager so chunk visibility and
        // sectionStorage stay in sync. Direct entityTickList mutation bypasses the
        // manager and leaves a stale entry when the chunk later stops ticking
        // (manager calls stopTicking -> onTickingEnd removes from ticker, not from
        // vanilla list). Try manager first; fall back to direct list for synthetic
        // test levels where the manager is not tracking the entity yet.
        try {
            var accessor = (ServerLevelAccessor) this.level;
            var manager = accessor.optimizedcushions$getEntityManager();
            ((xyz.nikitacartes.optimizedcushions.mixin.server.PersistentEntitySectionManagerAccessor) manager)
                .optimizedcushions$startTicking(cushion);
            // startTicking fires EntityCallbacks.onTickingStart which for a ridden
            // cushion is allowed to proceed and adds to entityTickList. Ensure presence
            // in case the manager considered it already ticking.
            var list = accessor.optimizedcushions$getEntityTickList();
            if (!list.contains(cushion)) {
                list.add(cushion);
            }
        } catch (Exception ignored) {
            ((ServerLevelAccessor) this.level).optimizedcushions$getEntityTickList().add(cushion);
        }
    }

    public void demoteIfIdle(final Cushion cushion) {
        CushionServerExt ext = (CushionServerExt) cushion;
        if (ext.optimizedcushions$isInTicker()) {
            return;
        }
        if (cushion.isPassenger() || !cushion.getPassengers().isEmpty()) {
            return;
        }
        // Queue for reclaim on next tick to avoid CME with entityTickList.forEach and to fix OC-02 leak
        // where demotion outside isServerTicking was a silent no-op. Processing in tick() handles both.
        pendingDemote.add(cushion);
    }

    public void tick() {
        // Reclaim queued idle cushions (OC-02/OC-23)
        if (!pendingDemote.isEmpty()) {
            Object[] pending = pendingDemote.toArray();
            pendingDemote.clear();
            for (Object o : pending) {
                if (o == null) {
                    continue;
                }
                Cushion cushion = (Cushion) o;
                if (cushion.isRemoved()) {
                    continue;
                }
                if (cushion.isPassenger() || !cushion.getPassengers().isEmpty()) {
                    continue;
                }
                CushionServerExt ext = (CushionServerExt) cushion;
                if (ext.optimizedcushions$isInTicker()) {
                    continue;
                }
                var accessor = (ServerLevelAccessor) this.level;
                var list = accessor.optimizedcushions$getEntityTickList();
                if (!list.contains(cushion)) {
                    continue;
                }
                // Prefer manager stopTicking so visibility tracking stays consistent (OC-23).
                // stopTicking triggers EntityCallbacks.onTickingEnd -> vanilla list remove.
                // If manager does not know the entity (promoted via fallback), directly remove.
                try {
                    var manager = accessor.optimizedcushions$getEntityManager();
                    ((xyz.nikitacartes.optimizedcushions.mixin.server.PersistentEntitySectionManagerAccessor) manager)
                        .optimizedcushions$stopTicking(cushion);
                    if (list.contains(cushion)) {
                        list.remove(cushion);
                    }
                } catch (Exception ignored) {
                    list.remove(cushion);
                }
                this.add(cushion);
            }
        }

        int count = this.cushions.size();
        if (count == 0) {
            return;
        }
        if (((xyz.nikitacartes.optimizedcushions.mixin.server.ServerLevelAccessor) this.level).optimizedcushions$getEmptyTime() >= 300) {
            return;
        }
        // OC-14: global runsNormally is a fast-path; per-entity isEntityFrozen is authoritative
        // for tick-freeze (/tick freeze) which can freeze non-player entities individually.
        // Cushions are never player-associated today so the two are equivalent, but we check
        // per-entity inside the loop for forward-compat.
        if (!this.level.tickRateManager().runsNormally()) {
            return;
        }

        if (this.iterationBuffer.length < count) {
            this.iterationBuffer = new Object[count + (count >> 1)];
        }
        Object[] buffer = this.cushions.toArray(this.iterationBuffer);
        // Clear tail to avoid stale references (OC-42)
        if (buffer.length > count) {
            Arrays.fill(buffer, count, buffer.length, null);
        }
        DistanceManager distanceManager = this.level.getChunkSource().chunkMap.getDistanceManager();

        for (int i = 0; i < count; i++) {
            Cushion cushion = (Cushion) buffer[i];
            buffer[i] = null;
            if (cushion.isRemoved()) {
                this.remove(cushion);
                continue;
            }
            if (!cushion.getPassengers().isEmpty() || cushion.isPassenger()) {
                this.promoteToVanilla(cushion);
                continue;
            }
            // OC-14: per-entity freeze. runsNormally() above is global fast-path; isEntityFrozen
            // is authoritative when Mojang adds selective freeze for non-players.
            if (this.level.tickRateManager().isEntityFrozen(cushion)) {
                continue;
            }
            // Freeze fully outside entity-ticking range like vanilla (OC-22)
            if (!distanceManager.inEntityTickingRange(cushion.chunkPosition().pack())) {
                continue;
            }
            // Replicate commonTick invulnerableTime-- (OC-04) before baseTick
            var entityAccessor = (xyz.nikitacartes.optimizedcushions.mixin.server.EntityAccessor) cushion;
            if (entityAccessor.optimizedcushions$getInvulnerableTime() > 0) {
                entityAccessor.optimizedcushions$setInvulnerableTime(entityAccessor.optimizedcushions$getInvulnerableTime() - 1);
            }
            cushion.setOldPosAndRot();
            cushion.tickCount++;
            this.level.guardEntityTick(TICK_ACTION, cushion);
        }
    }
}
