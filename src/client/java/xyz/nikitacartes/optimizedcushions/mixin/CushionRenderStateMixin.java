//? if >=1.21.5 {
package xyz.nikitacartes.optimizedcushions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimizedcushions.CushionRenderStateExt;

@Mixin(targets = "com.leclowndu93150.cushionbackport.client.CushionRenderState")
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
//?}
