package xyz.nikitacartes.optimizedcushions.mixin.server;

import java.util.List;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerEntity.class)
public interface ServerEntityAccessor {
    @Accessor("lastPassengers")
    List<Entity> optimizedcushions$getLastPassengers();
}
