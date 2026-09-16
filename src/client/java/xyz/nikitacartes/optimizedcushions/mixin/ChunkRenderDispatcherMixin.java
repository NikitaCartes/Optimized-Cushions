//? if <1.20.2 {
/*package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class ChunkRenderDispatcherMixin {
    private static final Logger optimizedcushions$LOGGER = LoggerFactory.getLogger("optimizedcushionsbackport");

    @Inject(
        method = "compile",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z", ordinal = 0)
    )
    private void optimizedcushions$bakeCushions(
        final float camX,
        final float camY,
        final float camZ,
        final ChunkBufferBuilderPack pack,
        final CallbackInfoReturnable<?> cir,
        final @Local Set<RenderType> startedLayers,
        final @Local RenderChunkRegion region,
        final @Local(ordinal = 0) BlockPos origin
    ) {
        Map<Integer, CushionTracker.Snapshot> cushions = CushionTracker.getForSection(SectionPos.asLong(origin));
        if (cushions == null || cushions.isEmpty()) {
            return;
        }

        BufferBuilder buffer = pack.builder(RenderType.cutout());
        if (startedLayers.add(RenderType.cutout())) {
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        }

        SectionPos sectionPos = SectionPos.of(origin);
        for (CushionTracker.Snapshot cushion : new ArrayList<>(cushions.values())) {
            try {
                CushionBaker.emit(buffer, cushion, sectionPos, region);
            } catch (Exception e) {
                optimizedcushions$LOGGER.warn("Failed to bake cushion {}", cushion, e);
            }
        }

        CushionSectionTasks.addTask(SectionPos.asLong(origin), () -> CushionTracker.commitBakedSection(SectionPos.asLong(origin)));
    }
}
*///?}
