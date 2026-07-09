package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.nikitacartes.optimizedcushions.server.TrackedEntityExt;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements TrackedEntityExt {
    @Shadow
    @Final
    private ServerEntity serverEntity;

    @Shadow
    @Final
    private Entity entity;

    @Override
    public Entity optimizedcushions$entity() {
        return this.entity;
    }

    @Override
    public ServerEntity optimizedcushions$serverEntity() {
        return this.serverEntity;
    }
}
