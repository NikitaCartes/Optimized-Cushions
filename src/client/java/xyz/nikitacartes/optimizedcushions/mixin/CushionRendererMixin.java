//? if >=1.21.5 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
//? if >=26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else
/*import net.minecraft.client.renderer.state.CameraRenderState;*/
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionRenderStateExt;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

@Mixin(targets = "com.leclowndu93150.cushionbackport.client.CushionRenderer")
public abstract class CushionRendererMixin extends EntityRenderer<Entity, EntityRenderState> {
    protected CushionRendererMixin(final EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void optimizedcushions$flagBaked(final Entity cushion, final EntityRenderState state, final float partialTicks, final CallbackInfo ci) {
        ((CushionRenderStateExt) state).optimizedcushions$setBaked(CushionTracker.isBaked(cushion));
    }

    @Inject(
        //? if >=26.1 {
        method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        //?} else
        /*method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",*/
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizedcushions$skipModel(
        final EntityRenderState state,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final CameraRenderState camera,
        final CallbackInfo ci
    ) {
        if (((CushionRenderStateExt) state).optimizedcushions$isBaked()) {
            //? if >=26.1 {
            this.submitNameDisplay(state, poseStack, submitNodeCollector, camera);
            //?} else
            /*this.submitNameTag(state, poseStack, submitNodeCollector, camera);*/
            ci.cancel();
        }
    }
}
//?}
