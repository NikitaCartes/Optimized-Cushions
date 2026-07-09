package xyz.nikitacartes.optimisedcushions.mixin.server;

import net.minecraft.world.entity.decoration.Cushion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.optimisedcushions.server.CushionServerExt;

@Mixin(Cushion.class)
public class CushionMixin implements CushionServerExt {
    @Unique
    private boolean optimisedcushions$serverTicking;

    @Unique
    private boolean optimisedcushions$inTicker;

    @Override
    public boolean optimisedcushions$isServerTicking() {
        return this.optimisedcushions$serverTicking;
    }

    @Override
    public void optimisedcushions$setServerTicking(final boolean ticking) {
        this.optimisedcushions$serverTicking = ticking;
    }

    @Override
    public boolean optimisedcushions$isInTicker() {
        return this.optimisedcushions$inTicker;
    }

    @Override
    public void optimisedcushions$setInTicker(final boolean inTicker) {
        this.optimisedcushions$inTicker = inTicker;
    }
}
