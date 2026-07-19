//? if >=1.20.2 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.Map;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
//? if >=1.21.11 {
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

// Present on 1.20.2+ (SectionRenderDispatcher era); 1.20.1 bakes via ChunkRenderDispatcherMixin.
// The section layer is keyed by ChunkSectionLayer on 1.21.11+ and by RenderType below it, and the
// region type is RenderSectionRegion vs RenderChunkRegion respectively.
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    // SectionCompiler's own helper for lazily starting a layer's BufferBuilder; using it keeps the
    // buffer construction (topology, vertex format) out of our code and portable across versions.
    //? if >=1.21.11 {
    @Shadow
    protected abstract BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, ChunkSectionLayer layer);
    //?} else {
    /*@Shadow
    protected abstract BufferBuilder getOrBeginLayer(Map<RenderType, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, RenderType layer);
    *///?}

    /**
     * Runs after the block loop, right before the started layers are built into meshes,
     * and appends the quads of every baked cushion in this section to the CUTOUT layer.
     */
    @Inject(
        method = "compile",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 0)
    )
    private void optimizedcushions$bakeCushions(
        final SectionPos sectionPos,
        //? if >=1.21.11 {
        final RenderSectionRegion region,
        //?} else
        /*final RenderChunkRegion region,*/
        final VertexSorting vertexSorting,
        final SectionBufferBuilderPack builders,
        final CallbackInfoReturnable<SectionCompiler.Results> cir,
        // By ordinal, not name: the local is `startedLayers` on 26.x but `map` below; it is the only
        // Map<layer, BufferBuilder> in compile(), so ordinal 0 is unambiguous.
        //? if >=1.21.11 {
        final @Local(ordinal = 0) Map<ChunkSectionLayer, BufferBuilder> startedLayers
        //?} else
        /*final @Local(ordinal = 0) Map<RenderType, BufferBuilder> startedLayers*/
    ) {
        Map<Integer, CushionTracker.Snapshot> cushions = CushionTracker.getForSection(sectionPos.asLong());
        if (cushions == null || cushions.isEmpty()) {
            return;
        }

        //? if >=1.21.11 {
        BufferBuilder builder = getOrBeginLayer(startedLayers, builders, ChunkSectionLayer.CUTOUT);
        //?} else
        /*BufferBuilder builder = getOrBeginLayer(startedLayers, builders, RenderType.cutout());*/

        for (CushionTracker.Snapshot cushion : cushions.values()) {
            CushionBaker.emit(builder, cushion, sectionPos, region);
        }
    }
}
//?}
