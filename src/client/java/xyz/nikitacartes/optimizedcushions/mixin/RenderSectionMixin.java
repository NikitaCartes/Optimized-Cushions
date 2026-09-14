package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class RenderSectionMixin {
    @Inject(method = "setSectionMesh", at = @At("RETURN"))
    private void optimizedcushions$commitCushions(final CallbackInfoReturnable<SectionMesh> cir) {
        SectionRenderDispatcher.RenderSection self = (SectionRenderDispatcher.RenderSection)(Object)this;
        CushionSectionTasks.executeTasks(SectionPos.asLong(self.getRenderOrigin()));
    }
}
