package xyz.nikitacartes.optimisedcushions.mixin;

import net.minecraft.client.renderer.entity.state.CushionRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimisedcushions.CushionRenderStateExt;

@Mixin(CushionRenderState.class)
public class CushionRenderStateMixin implements CushionRenderStateExt {
    @Unique
    private boolean optimisedcushions$baked;

    @Override
    public void optimisedcushions$setBaked(final boolean baked) {
        this.optimisedcushions$baked = baked;
    }

    @Override
    public boolean optimisedcushions$isBaked() {
        return this.optimisedcushions$baked;
    }
}
