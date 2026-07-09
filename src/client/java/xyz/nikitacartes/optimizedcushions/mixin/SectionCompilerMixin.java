package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.util.Map;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
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
        final RenderSectionRegion region,
        final VertexSorting vertexSorting,
        final SectionBufferBuilderPack builders,
        final CallbackInfoReturnable<SectionCompiler.Results> cir,
        final @Local(name = "startedLayers") Map<ChunkSectionLayer, BufferBuilder> startedLayers
    ) {
        Map<Integer, CushionTracker.Snapshot> cushions = CushionTracker.getForSection(sectionPos.asLong());
        if (cushions == null || cushions.isEmpty()) {
            return;
        }

        BufferBuilder builder = startedLayers.computeIfAbsent(
            ChunkSectionLayer.CUTOUT,
            layer -> new BufferBuilder(builders.buffer(layer), PrimitiveTopology.QUADS, layer.vertexFormat())
        );

        for (CushionTracker.Snapshot cushion : cushions.values()) {
            CushionBaker.emit(builder, cushion, sectionPos, region);
        }
    }
}
