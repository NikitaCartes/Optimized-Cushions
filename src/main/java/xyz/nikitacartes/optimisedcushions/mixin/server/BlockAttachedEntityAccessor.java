package xyz.nikitacartes.optimisedcushions.mixin.server;

import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockAttachedEntity.class)
public interface BlockAttachedEntityAccessor {
    @Accessor("ticksSinceLastCheck")
    int optimisedcushions$getTicksSinceLastCheck();

    @Accessor("ticksSinceLastCheck")
    void optimisedcushions$setTicksSinceLastCheck(int ticks);
}
