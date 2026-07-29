//? if <1.21.5 {
/*package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

// Pre-render-state era (< 1.21.5): the backport renderer has no render state, so we key on the
// entity directly. Cancel the whole render for baked cushions - their model is in the chunk mesh.
@Mixin(targets = "com.leclowndu93150.cushionbackport.client.CushionRenderer")
public class CushionRendererOldMixin {
    // Target EntityRenderer's erased (bridge) signature, not the backport's typed render(Cushion, …)
    // override: only the Minecraft supertype carries the obfuscation mapping the refmap AP needs.
    @Inject(
        method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizedcushions$skipBaked(
        final Entity cushion,
        final float yaw,
        final float partialTicks,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final CallbackInfo ci
    ) {
        if (CushionTracker.isBaked(cushion)) {
            ci.cancel();
        }
    }
}
*///?}
