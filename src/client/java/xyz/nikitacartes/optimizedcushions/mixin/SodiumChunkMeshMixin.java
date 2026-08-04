//? if >=1.21.1 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
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
import xyz.nikitacartes.optimizedcushions.CushionBaker;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

/**
 * Bakes cushions into Sodium's chunk meshes at {@code runChunkMeshAppenders}, its per-section
 * appender hook, emitting into CUTOUT like {@code SectionCompilerMixin} does for the vanilla path.
 * {@code @Pseudo} + string targets: only one platform hook class exists per node, and none when
 * Sodium is absent.
 */
@Pseudo
@Mixin(targets = {
    "net.caffeinemc.mods.sodium.fabric.level.FabricLevelRenderHooks",
    "net.caffeinemc.mods.sodium.neoforge.level.NeoForgeLevelRenderHooks"
}, remap = false)
public class SodiumChunkMeshMixin {
    @Inject(method = "runChunkMeshAppenders", at = @At("HEAD"), remap = false)
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
        /*// Pre-26.1 the hook omits the origin; recover the section's min corner from the slice's private
        // originBlockX/Y/Z, which sit one chunk (+16) below.
        LevelSliceOriginAccessor acc = (LevelSliceOriginAccessor) (Object) slice;
        BlockPos origin = new BlockPos(acc.optimizedcushions$originBlockX() + 16, acc.optimizedcushions$originBlockY() + 16, acc.optimizedcushions$originBlockZ() + 16);
        *///?}
        Map<Integer, CushionTracker.Snapshot> cushions = CushionTracker.getForSection(SectionPos.asLong(origin));
        if (cushions == null || cushions.isEmpty()) {
            return;
        }

        //? if >=1.21.11 {
        VertexConsumer buffer = typeToConsumer.apply(ChunkSectionLayer.CUTOUT);
        //?} else
        /*VertexConsumer buffer = typeToConsumer.apply(RenderType.cutout());*/
        SectionPos sectionPos = SectionPos.of(origin);
        for (CushionTracker.Snapshot cushion : cushions.values()) {
            CushionBaker.emitFallback(buffer, cushion, sectionPos, slice);
        }
    }
}
//?}
