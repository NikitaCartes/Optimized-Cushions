//? if >=1.20.2 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
//? if >=26.2 {
import com.mojang.blaze3d.PrimitiveTopology;
//?} else {
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
*///?}
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
    // Local replicate of SectionCompiler.getOrBeginLayer: the legacy Mixin AP emits no
    // @Shadow-method refmap entries, so @Shadow of this vanilla method breaks in obfuscated
    // production. The body below is identical to vanilla (get-or-create + begin QUADS/BLOCK)
    // and uses only stable public APIs. Kept @Unique so it can never clash with the real one.
    //? if >=26.2 {
    @Unique
    private BufferBuilder optimizedcushions$getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, ChunkSectionLayer layer) {
        BufferBuilder builder = startedLayers.get(layer);
        if (builder == null) {
            ByteBufferBuilder buffer = builders.buffer(layer);
            builder = new BufferBuilder(buffer, PrimitiveTopology.QUADS, layer.vertexFormat());
            startedLayers.put(layer, builder);
        }
        return builder;
    }
    //?} elif >=26.1 {
    /*@Unique
    private BufferBuilder optimizedcushions$getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, ChunkSectionLayer layer) {
        BufferBuilder builder = startedLayers.get(layer);
        if (builder == null) {
            ByteBufferBuilder buffer = builders.buffer(layer);
            builder = new BufferBuilder(buffer, VertexFormat.Mode.QUADS, layer.vertexFormat());
            startedLayers.put(layer, builder);
        }
        return builder;
    }
    *///?} elif >=1.21.11 {
    /*@Unique
    private BufferBuilder optimizedcushions$getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, ChunkSectionLayer layer) {
        BufferBuilder builder = startedLayers.get(layer);
        if (builder == null) {
            ByteBufferBuilder buffer = builders.buffer(layer);
            builder = new BufferBuilder(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            startedLayers.put(layer, builder);
        }
        return builder;
    }
    *///?} else {
    /*@Unique
    private BufferBuilder optimizedcushions$getOrBeginLayer(Map<RenderType, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, RenderType layer) {
        BufferBuilder builder = startedLayers.get(layer);
        if (builder == null) {
            ByteBufferBuilder buffer = builders.buffer(layer);
            builder = new BufferBuilder(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            startedLayers.put(layer, builder);
        }
        return builder;
    }
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
        BufferBuilder builder = optimizedcushions$getOrBeginLayer(startedLayers, builders, ChunkSectionLayer.CUTOUT);
        //?} else
        /*BufferBuilder builder = optimizedcushions$getOrBeginLayer(startedLayers, builders, RenderType.cutout());*/

        // Snapshot copy: BY_SECTION inner maps are live ConcurrentHashMaps, so a worker
        // iterating values() directly could bake a torn membership set.
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
