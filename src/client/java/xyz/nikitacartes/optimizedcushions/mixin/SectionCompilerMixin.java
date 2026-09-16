//? if >=1.20.2 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    //? if >=1.21.11 {
    @Shadow
    protected abstract BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, ChunkSectionLayer layer);
    //?} else {
    /*@Shadow
    protected abstract BufferBuilder getOrBeginLayer(Map<RenderType, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, RenderType layer);
    *///?}

    @Unique
    private static final Logger optimizedcushions$LOGGER = LoggerFactory.getLogger("optimizedcushionsbackport");

    @Inject(
        //? if neoforge && >=1.21.11 {
        /*method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        *///?} elif neoforge {
        /*method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        *///?} else {
        method = "compile",
        //?}
        at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 0),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;"),
            to = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;")
        )
    )
    private void optimizedcushions$bakeCushions(
        final SectionPos sectionPos,
        //? if >=1.21.11 {
        final RenderSectionRegion region,
        //?} else
        /*final RenderChunkRegion region,*/
        final VertexSorting vertexSorting,
        final SectionBufferBuilderPack builders,
        //? if neoforge {
        /*final java.util.List<?> additionalRenderers,
        *///?}
        final CallbackInfoReturnable<SectionCompiler.Results> cir,
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

        for (CushionTracker.Snapshot cushion : new ArrayList<>(cushions.values())) {
            try {
                CushionBaker.emit(builder, cushion, sectionPos, region);
            } catch (Exception e) {
                optimizedcushions$LOGGER.warn("Failed to bake cushion {}", cushion, e);
            }
        }

        CushionSectionTasks.addTask(sectionPos.asLong(), () -> CushionTracker.commitBakedSection(sectionPos.asLong()));
    }
}
//?}
