package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

/**
 * Commits deferred baked-flag flips when Sodium installs a rebuilt section mesh. Sodium replaces
 * the vanilla RenderSection, so {@code RenderSectionMixin} never fires under it. {@code setInfo}
 * runs on the main thread once per build result; null means section removal and is skipped.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSection", remap = false)
public class SodiumRenderSectionMixin {
    @Shadow @Final private int chunkX;
    @Shadow @Final private int chunkY;
    @Shadow @Final private int chunkZ;

    @Inject(method = "setInfo", at = @At("RETURN"), remap = false)
    private void optimizedcushions$commitCushions(final @Coerce Object info, final CallbackInfoReturnable<?> cir) {
        if (info != null) {
            long sectionKey = SectionPos.asLong(this.chunkX, this.chunkY, this.chunkZ);
            CushionSectionTasks.executeTasks(sectionKey);
            // Sodium never meshes air-only sections; a mesh without block geometry
            // (MASK_HAS_BLOCK_GEOMETRY, bit 0) holds no cushion copy.
            if ((((SodiumBuiltSectionInfoAccessor)info).optimizedcushions$flags() & 1) == 0) {
                CushionTracker.commitEmptySection(sectionKey);
            }
        }
    }
}
