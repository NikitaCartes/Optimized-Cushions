//? if >=1.20.2 {
package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=1.21.11 {
import net.minecraft.client.renderer.chunk.SectionMesh;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class RenderSectionMixin {
    //? if >=1.21.11 {
    @Inject(method = "setSectionMesh", at = @At("RETURN"))
    private void optimizedcushions$commitCushions(final CallbackInfoReturnable<SectionMesh> cir) {
        SectionRenderDispatcher.RenderSection self = (SectionRenderDispatcher.RenderSection) (Object) this;
        BlockPos origin = self.getRenderOrigin();
        CushionSectionTasks.executeTasks(SectionPos.asLong(origin));
    }
    //?} else {
    /*@Inject(method = "setCompiled", at = @At("RETURN"))
    private void optimizedcushions$commitCushions(final CallbackInfo ci) {
        SectionRenderDispatcher.RenderSection self = (SectionRenderDispatcher.RenderSection) (Object) this;
        BlockPos origin = self.getOrigin();
        CushionSectionTasks.executeTasks(SectionPos.asLong(origin));
    }
    *///?}
}
//?}
