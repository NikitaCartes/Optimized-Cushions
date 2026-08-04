package xyz.nikitacartes.optimizedcushions.mixin.server;

//? if >=1.21
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// The 100-tick support-recheck counter. >=1.21 targets vanilla BlockAttachedEntity; below that the
// backport ships its own. Field is checkInterval on both: Mojang renamed it only at 26.3.
//? if >=1.21 {
@Mixin(BlockAttachedEntity.class)
//?} else
/*@Mixin(targets = "com.leclowndu93150.cushionbackport.entity.BlockAttachedEntity")*/
public interface BlockAttachedEntityAccessor {
    @Accessor("checkInterval")
    int optimizedcushions$getTicksSinceLastCheck();

    @Accessor("checkInterval")
    void optimizedcushions$setTicksSinceLastCheck(int ticks);
}
