package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.optimizedcushions.CushionSectionTasks;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "removeTransientBlocksInSection", at = @At("RETURN"), require = 0)
    private void optimizedcushions$commitCushions(final long sectionNode, final long compileTaskStartTimeNs, final CallbackInfo ci) {
        CushionSectionTasks.executeTasks(sectionNode);
    }
}
