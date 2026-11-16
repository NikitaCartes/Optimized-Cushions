//? if <1.21.2 {
/*package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimizedcushions.server.CushionServerExt;

// String target: VanillaBackport has no 1.21.11+ files; gated by plugin.
@Mixin(targets = "com.blackgear.vanillabackport.common.level.entities.decoration.Cushion")
public abstract class VanillaCushionMixin implements CushionServerExt {
    @Shadow
    public abstract DyeColor getColor();

    @Shadow
    public abstract void setColor(DyeColor color);
    @Unique
    private boolean optimizedcushions$serverTicking;

    @Unique
    private boolean optimizedcushions$inTicker;

    @Override
    public boolean optimizedcushions$isServerTicking() {
        return this.optimizedcushions$serverTicking;
    }

    @Override
    public void optimizedcushions$setServerTicking(final boolean ticking) {
        this.optimizedcushions$serverTicking = ticking;
    }

    @Override
    public boolean optimizedcushions$isInTicker() {
        return this.optimizedcushions$inTicker;
    }

    @Override
    public void optimizedcushions$setInTicker(final boolean inTicker) {
        this.optimizedcushions$inTicker = inTicker;
    }

    @Override
    public DyeColor optimizedcushions$color() {
        return this.getColor();
    }

    @Override
    public void optimizedcushions$setColor(final DyeColor color) {
        this.setColor(color);
    }
}
*///?}
