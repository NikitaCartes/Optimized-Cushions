package xyz.nikitacartes.optimizedcushions.server;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.function.Consumer;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import xyz.nikitacartes.optimizedcushions.OptCushion;
import xyz.nikitacartes.optimizedcushions.mixin.server.BlockAttachedEntityAccessor;
import xyz.nikitacartes.optimizedcushions.mixin.server.ServerLevelAccessor;

public final class CushionServerTicker {
    private static final Consumer<Entity> TICK_ACTION = Entity::tick;
    private static final int CHECK_INTERVAL = 100;

    private final ServerLevel level;
    private final ReferenceLinkedOpenHashSet<Entity> cushions = new ReferenceLinkedOpenHashSet<>();
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

    public void promoteToVanilla(final Entity cushion) {
        this.remove(cushion);
        ((ServerLevelAccessor) this.level).optimizedcushions$getEntityTickList().add(cushion);
    }

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

    public void tick() {
        int count = this.cushions.size();
        if (count == 0) {
            return;
        }
        if (((ServerLevelAccessor) this.level).optimizedcushions$getEmptyTime() >= 300) {
            return;
        }
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
            if (!cushion.getPassengers().isEmpty() || cushion.isPassenger()) {
                this.promoteToVanilla(cushion);
                continue;
            }
            //? if >=1.20.3 {
            if (this.level.tickRateManager().isEntityFrozen(cushion)) {
                continue;
            }
            //?}
            BlockAttachedEntityAccessor accessor = (BlockAttachedEntityAccessor) cushion;
            //? if >=26.1 {
            long chunkKey = cushion.chunkPosition().pack();
            //?} else
            /*long chunkKey = cushion.chunkPosition().toLong();*/
            if (accessor.optimizedcushions$getTicksSinceLastCheck() >= CHECK_INTERVAL
                    && !distanceManager.inEntityTickingRange(chunkKey)) {
                accessor.optimizedcushions$setTicksSinceLastCheck(0);
                continue;
            }
            cushion.setOldPosAndRot();
            cushion.tickCount++;
            this.level.guardEntityTick(TICK_ACTION, cushion);
        }
    }
}
