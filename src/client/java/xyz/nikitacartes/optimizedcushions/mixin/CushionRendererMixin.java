//? if >=1.21.5 {
package xyz.nikitacartes.optimizedcushions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
//? if >=26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else
/*import net.minecraft.client.renderer.state.CameraRenderState;*/
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionRenderStateExt;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

// String target + @Coerce/@Shadow because the backport's renderer/state/entity types are not on
// our compile classpath. Descriptors are written by internal name so mixin can resolve them.
// The name-display method and CameraRenderState package changed at 26.1 (submitNameTag ->
// submitNameDisplay; renderer.state.CameraRenderState -> renderer.state.level.CameraRenderState).
@Mixin(targets = "com.leclowndu93150.cushionbackport.client.CushionRenderer")
public abstract class CushionRendererMixin {
    //? if >=26.1 {
    @Shadow
    protected abstract void submitNameDisplay(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);
    //?} else {
    /*@Shadow
    protected abstract void submitNameTag(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);
    *///?}

    @Inject(
        method = "extractRenderState(Lcom/leclowndu93150/cushionbackport/entity/Cushion;Lcom/leclowndu93150/cushionbackport/client/CushionRenderState;F)V",
        at = @At("TAIL")
    )
    private void optimizedcushions$flagBaked(final @Coerce Entity cushion, final @Coerce EntityRenderState state, final float partialTicks, final CallbackInfo ci) {
        ((CushionRenderStateExt) state).optimizedcushions$setBaked(CushionTracker.isBaked(cushion));
    }

    @Inject(
        //? if >=26.1 {
        method = "submit(Lcom/leclowndu93150/cushionbackport/client/CushionRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        //?} else
        /*method = "submit(Lcom/leclowndu93150/cushionbackport/client/CushionRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",*/
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizedcushions$skipModel(
        final @Coerce EntityRenderState state,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final CameraRenderState camera,
        final CallbackInfo ci
    ) {
        if (((CushionRenderStateExt) state).optimizedcushions$isBaked()) {
            // The cushion is part of the chunk mesh; keep only the name tag (cushions are not Leashable).
            //? if >=26.1 {
            this.submitNameDisplay(state, poseStack, submitNodeCollector, camera);
            //?} else
            /*this.submitNameTag(state, poseStack, submitNodeCollector, camera);*/
            ci.cancel();
        }
    }
}
//?}
