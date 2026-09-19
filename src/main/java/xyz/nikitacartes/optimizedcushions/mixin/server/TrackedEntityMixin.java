package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import xyz.nikitacartes.optimizedcushions.server.TrackedEntityExt;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements TrackedEntityExt {
    @Override
    public Entity optimizedcushions$entity() {
        return ((TrackedEntityAccessor) this).optimizedcushions$accessEntity();
    }

    @Override
    public ServerEntity optimizedcushions$serverEntity() {
        return ((TrackedEntityAccessor) this).optimizedcushions$accessServerEntity();
    }
}
