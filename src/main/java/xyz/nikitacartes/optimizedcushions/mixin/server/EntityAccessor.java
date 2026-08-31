package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("invulnerableTime")
    int optimizedcushions$getInvulnerableTime();

    @Accessor("invulnerableTime")
    void optimizedcushions$setInvulnerableTime(int time);
}
