//? if <1.20.2 {
package xyz.nikitacartes.optimizedcushions.mixin;

/*import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk")
public class ChunkRenderCommitMixin {
    @Inject(method = "setCompiledChunk", at = @At("RETURN"), require = 0)
    private void optimizedcushions$commitCushions(final CallbackInfo ci) {
        try {
            Object self = (Object) this;
            BlockPos origin = ((net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk) self).getOrigin();
            CushionSectionTasks.executeTasks(SectionPos.asLong(origin));
        } catch (Throwable ignored) {
        }
    }
}
*///?}
