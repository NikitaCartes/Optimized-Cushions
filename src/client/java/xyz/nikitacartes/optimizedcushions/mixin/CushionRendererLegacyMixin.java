//? if <1.21 {
/*package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

// 1.20.1 variant of CushionRendererOldMixin: renderNameTag takes no partialTick here.
//
// Extends EntityRenderer so shouldShowName/renderNameTag resolve through the vanilla
// superclass: the legacy Mixin AP emits no @Shadow-method refmap entries, and the backport
// renderer does not redeclare those EntityRenderer members, so @Shadow of them breaks in
// obfuscated production.
@Mixin(targets = "com.leclowndu93150.cushionbackport.client.CushionRenderer")
public abstract class CushionRendererLegacyMixin extends EntityRenderer<Entity> {
    protected CushionRendererLegacyMixin(final EntityRendererProvider.Context context) {
        super(context);
    }

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
            // The model is in the chunk mesh; keep the nameplate like the >=1.21.5 path does.
            if (this.shouldShowName(cushion)) {
                this.renderNameTag(cushion, cushion.getDisplayName(), poseStack, bufferSource, packedLight);
            }
            ci.cancel();
        }
    }
}
*///?}
