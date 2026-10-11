package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    private static final Logger optimizedcushions$LOGGER = LoggerFactory.getLogger("optimizedcushions");

    // require = 0: on hook signature change degrade to vanilla entities, never crash.
    @Inject(method = "runChunkMeshAppenders", at = @At("HEAD"), remap = false, require = 0)
    private void optimizedcushions$bakeCushions(
            final List<?> renderers,
            final Function<ChunkSectionLayer, VertexConsumer> typeToConsumer,
            final @Coerce BlockAndTintGetter slice,
            final BlockPos origin,
            final CallbackInfo ci
    ) {
        final long sectionKey = SectionPos.asLong(origin);
        Map<Integer, CushionTracker.Snapshot> cushions = CushionTracker.getForSection(sectionKey);
        if (cushions == null || cushions.isEmpty()) {
            return;
        }

        final VertexConsumer builder;
        try {
            builder = typeToConsumer.apply(ChunkSectionLayer.CUTOUT);
        } catch (Exception e) {
            return;
        }
        if (builder == null) {
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
                CushionBaker.emit(builder, cushion, sectionPos, slice);
            } catch (Exception e) {
                optimizedcushions$LOGGER.warn("Failed to bake cushion {}", cushion, e);
            }
        }

        CushionSectionTasks.addTask(sectionKey, () -> CushionTracker.commitBakedSection(sectionKey));
    }
}
