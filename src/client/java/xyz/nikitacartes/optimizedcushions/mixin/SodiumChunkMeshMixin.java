//? if >=1.21.1 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
//? if >=1.21.11 {
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/
//? if >=26.1 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//?} else
/*import net.minecraft.world.level.BlockAndTintGetter;*/
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Pseudo
@Mixin(targets = {
    "net.caffeinemc.mods.sodium.fabric.level.FabricLevelRenderHooks",
    "net.caffeinemc.mods.sodium.neoforge.level.NeoForgeLevelRenderHooks"
}, remap = false)
public class SodiumChunkMeshMixin {
    private static final Logger optimizedcushions$LOGGER = LoggerFactory.getLogger("optimizedcushionsbackport");

    @Inject(method = "runChunkMeshAppenders", at = @At("HEAD"), remap = false, require = 0)
    private void optimizedcushions$bakeCushions(
        final List<?> renderers,
        //? if >=1.21.11 {
        final Function<ChunkSectionLayer, VertexConsumer> typeToConsumer,
        //?} else
        /*final Function<RenderType, VertexConsumer> typeToConsumer,*/
        final @Coerce BlockAndTintGetter slice,
        //? if >=26.1 {
        final BlockPos origin,
        //?}
        final CallbackInfo ci
    ) {
        //? if <26.1 {
        /*LevelSliceOriginAccessor acc = (LevelSliceOriginAccessor) (Object) slice;
        BlockPos origin = new BlockPos(acc.optimizedcushions$originBlockX() + 16, acc.optimizedcushions$originBlockY() + 16, acc.optimizedcushions$originBlockZ() + 16);
        *///?}
        Map<Integer, CushionTracker.Snapshot> cushions = CushionTracker.getForSection(SectionPos.asLong(origin));
        if (cushions == null || cushions.isEmpty()) {
            return;
        }

        final VertexConsumer buffer;
        try {
            //? if >=1.21.11 {
            buffer = typeToConsumer.apply(ChunkSectionLayer.CUTOUT);
            //?} else
            /*buffer = typeToConsumer.apply(RenderType.cutout());*/
        } catch (Exception e) {
            return;
        }
        if (buffer == null) {
            return;
        }
        SectionPos sectionPos = SectionPos.of(origin);
        // Snapshot copy: BY_SECTION inner maps are live ConcurrentHashMaps, so a worker
        // iterating values() directly could bake a torn membership set.
        for (CushionTracker.Snapshot cushion : new ArrayList<>(cushions.values())) {
            try {
                CushionBaker.emitFallback(buffer, cushion, sectionPos, slice);
            } catch (Exception e) {
                optimizedcushions$LOGGER.warn("Failed to bake cushion {}", cushion, e);
            }
        }

        CushionSectionTasks.addTask(SectionPos.asLong(origin), () -> CushionTracker.commitBakedSection(SectionPos.asLong(origin)));
    }
}
//?}
