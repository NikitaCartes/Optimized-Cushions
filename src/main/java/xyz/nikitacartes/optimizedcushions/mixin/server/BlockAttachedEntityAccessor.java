package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockAttachedEntity.class)
public interface BlockAttachedEntityAccessor {
    @Accessor("ticksSinceLastCheck")
    int optimizedcushions$getTicksSinceLastCheck();

    @Accessor("ticksSinceLastCheck")
    void optimizedcushions$setTicksSinceLastCheck(int ticks);
}
