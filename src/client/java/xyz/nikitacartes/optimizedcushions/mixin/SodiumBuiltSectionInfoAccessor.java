//? if >=1.21.1 {
package xyz.nikitacartes.optimizedcushions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

// SodiumRenderSectionMixin reads the mesh flags without a compile-time Sodium dependency.
// @Pseudo: Sodium may be absent at runtime.
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo", remap = false)
public interface SodiumBuiltSectionInfoAccessor {
    @Accessor(value = "flags", remap = false) int optimizedcushions$flags();
}
//?}
