package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @Shadow
    protected abstract BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack builders, ChunkSectionLayer layer);

    private static final Logger optimizedcushions$LOGGER = LoggerFactory.getLogger("optimizedcushions");
    /**
     * Runs after the block loop, right before the started layers are built into meshes,
     * and appends the quads of every baked cushion in this section to the CUTOUT layer.
     */
    @Inject(
        method = "compile",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 0),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;"),
            to = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;")
        )
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

        BufferBuilder builder = getOrBeginLayer(startedLayers, builders, ChunkSectionLayer.CUTOUT);

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
