//? if >=1.21.1 <26.1 {
/*package xyz.nikitacartes.optimizedcushions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

// On 1.21.1/1.21.11 runChunkMeshAppenders omits the built section's origin, so SodiumChunkMeshMixin
// recovers it from these private LevelSlice fields. @Pseudo: Sodium may be absent at runtime.
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public interface LevelSliceOriginAccessor {
    @Accessor(value = "originBlockX", remap = false) int optimizedcushions$originBlockX();
    @Accessor(value = "originBlockY", remap = false) int optimizedcushions$originBlockY();
    @Accessor(value = "originBlockZ", remap = false) int optimizedcushions$originBlockZ();
}
*///?}
