//? if >=1.21 <1.21.5 {
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

// Pre-render-state era (1.21-1.21.11): key on entity; baked cushions skip render.
// 1.20.1 uses VanillaCushionRendererLegacyMixin (5-arg renderNameTag).
//
// String target (no 1.21.11+ files; gated by plugin). Extends EntityRenderer so the
// nameplate resolves via vanilla: @Shadow breaks in obfuscated production.
@Mixin(targets = "com.blackgear.vanillabackport.client.level.renderer.object.CushionRenderer")
public abstract class VanillaCushionRendererOldMixin extends EntityRenderer<Entity> {
    protected VanillaCushionRendererOldMixin(final EntityRendererProvider.Context context) {
        super(context);
    }

    // Bridge signature: only the vanilla supertype has refmap mappings.
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
            // Baked into chunk mesh; keep nameplate.
            if (this.shouldShowName(cushion)) {
                this.renderNameTag(cushion, cushion.getDisplayName(), poseStack, bufferSource, packedLight, partialTicks);
            }
            ci.cancel();
        }
    }
}
*///?}
