//? if <1.20.2 {
/*package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

// 1.20.1 has no SectionCompiler; chunk meshes are built by RebuildTask.compile. After the block
// loop, append baked cushion quads to the CUTOUT layer, beginning that layer first if no cutout
// block already started it. RenderChunk.beginLayer is just buffer.begin(QUADS, BLOCK), inlined here
// to avoid reaching the enclosing RenderChunk instance from this inner-class mixin.
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class ChunkRenderDispatcherMixin {
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
        for (CushionTracker.Snapshot cushion : cushions.values()) {
            CushionBaker.emit(buffer, cushion, sectionPos, region);
        }
    }
}
*///?}
