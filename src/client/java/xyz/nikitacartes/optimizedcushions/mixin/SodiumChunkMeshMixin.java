//? if >=1.21.1 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.Level;
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

/**
 * Bakes cushions into Sodium meshes via {@code LevelRenderHooks.runChunkMeshAppenders} head
 * (runs on the meshing worker). Vanilla-types-only signature, so no Sodium dependency;
 * {@code @Pseudo} skips this mixin when Sodium is absent.
 */
@Pseudo
@Mixin(targets = {
    "net.caffeinemc.mods.sodium.fabric.level.FabricLevelRenderHooks",
    "net.caffeinemc.mods.sodium.neoforge.level.NeoForgeLevelRenderHooks"
}, remap = false)
public class SodiumChunkMeshMixin {
    private static final Logger optimizedcushions$LOGGER = LoggerFactory.getLogger("optimizedcushionsbackport");

    // require = 0: on hook signature change degrade to vanilla entities, never crash.
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
        final SectionPos sectionPos = SectionPos.of(origin);
        // Copy: inner maps are live, direct iteration could bake a torn set.
        Level clientLevel = Minecraft.getInstance().level;
        for (CushionTracker.Snapshot cushion : new ArrayList<>(cushions.values())) {
            // Foreign level: bake, flag flip, and eviction each skip/handle it elsewhere.
            if (cushion.level() != clientLevel) {
                continue;
            }
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
