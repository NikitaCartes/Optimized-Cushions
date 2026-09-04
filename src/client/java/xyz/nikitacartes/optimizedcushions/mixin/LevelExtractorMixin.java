package xyz.nikitacartes.optimizedcushions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    // Named cushions keep the vanilla path so the name tag still renders (their model
    // submit is skipped in CushionRendererMixin). Wraps the call rather than
    // isEntityVisible itself so F3+B hitboxes still see baked cushions.
    @WrapOperation(
        method = "extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;isEntityVisible(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"
        )
    )
    private boolean optimizedcushions$skipBakedCushions(
        final LevelExtractor extractor,
        final Entity entity,
        final Frustum frustum,
        final double camX,
        final double camY,
        final double camZ,
        final Operation<Boolean> original
    ) {
        if (entity instanceof Cushion cushion && CushionTracker.isBaked(cushion) && !cushion.hasCustomName()) {
            return false;
        }
        return original.call(extractor, entity, frustum, camX, camY, camZ);
    }
}
