package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.client.renderer.entity.state.CushionRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimizedcushions.CushionRenderStateExt;

@Mixin(CushionRenderState.class)
public class CushionRenderStateMixin implements CushionRenderStateExt {
    @Unique
    private boolean optimizedcushions$baked;

    @Override
    public void optimizedcushions$setBaked(final boolean baked) {
        this.optimizedcushions$baked = baked;
    }

    @Override
    public boolean optimizedcushions$isBaked() {
        return this.optimizedcushions$baked;
    }
}
