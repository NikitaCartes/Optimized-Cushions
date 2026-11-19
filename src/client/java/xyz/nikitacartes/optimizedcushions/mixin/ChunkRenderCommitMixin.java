//? if <1.20.2 {
/*package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;

// 1.20.1 RenderChunk has no mesh setter: RebuildTask sets compiled, then reports the chunk here
// on the main thread. Sodium 0.5 never calls this, and the tracker is off under it anyway.
@Mixin(LevelRenderer.class)
public class ChunkRenderCommitMixin {
    @Inject(method = "addRecentlyCompiledChunk", at = @At("RETURN"))
    private void optimizedcushions$commitCushions(final ChunkRenderDispatcher.RenderChunk renderChunk, final CallbackInfo ci) {
        CushionSectionTasks.executeTasks(SectionPos.asLong(renderChunk.getOrigin()));
    }
}
*///?}
