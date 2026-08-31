package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentEntitySectionManager.class)
public interface PersistentEntitySectionManagerAccessor<T extends EntityAccess> {
    @Invoker("startTicking")
    void optimizedcushions$startTicking(T entity);

    @Invoker("stopTicking")
    void optimizedcushions$stopTicking(T entity);
}
