package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface ServerLevelAccessor {
    @Accessor("entityTickList")
    EntityTickList optimizedcushions$getEntityTickList();

    @Accessor("emptyTime")
    int optimizedcushions$getEmptyTime();

    @Accessor("entityManager")
    PersistentEntitySectionManager<Entity> optimizedcushions$getEntityManager();
}
