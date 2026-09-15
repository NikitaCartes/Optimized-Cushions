package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
 * Bakes cushions into Sodium chunk meshes.
 *
 * <p>On the Fabric loader Sodium's meshing task delegates its tail to
 * {@code FabricLevelRenderHooks.runChunkMeshAppenders}, which is otherwise a no-op:
 * injecting at its head runs on the meshing worker with the section's vertex-consumer
 * factory, level slice and section origin already resolved. Only vanilla types appear
 * in this signature (the Sodium slice is coerced to its {@code BlockAndTintGetter}
 * superinterface), so no Sodium dependency is needed and {@code @Pseudo} simply
 * skips this mixin when Sodium is absent.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.fabric.level.FabricLevelRenderHooks", remap = false)
public class SodiumChunkMeshMixin {
    private static final Logger optimizedcushions$LOGGER = LoggerFactory.getLogger("optimizedcushions");

    // require = 0: if Sodium ever changes this hook's signature, degrade to vanilla
    // entities instead of crashing. (@Pseudo only covers a missing target class,
    // not a changed method inside an existing class.)
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
        // Snapshot copy: BY_SECTION inner maps are live ConcurrentHashMaps, so a worker
        // iterating values() directly could bake a torn membership set.
        for (CushionTracker.Snapshot cushion : new ArrayList<>(cushions.values())) {
            try {
                CushionBaker.emit(builder, cushion, sectionPos, slice);
            } catch (Exception e) {
                optimizedcushions$LOGGER.warn("Failed to bake cushion {}", cushion, e);
            }
        }

        CushionSectionTasks.addTask(sectionKey, () -> CushionTracker.commitBakedSection(sectionKey));
    }
}
