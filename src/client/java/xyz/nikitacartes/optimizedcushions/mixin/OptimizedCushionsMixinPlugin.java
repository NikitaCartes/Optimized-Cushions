package xyz.nikitacartes.optimizedcushions.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import static xyz.nikitacartes.optimizedcushions.OptimizedCushionsClient.isObeLoaded;

/**
 * Skips all client mixins when OBE is present: OBE bakes cushions itself, so running
 * both would double-bake. The runtime guard in {@code OptimizedCushionsClient} alone
 * is not enough: with {@code required:true} the mixins would still apply and can
 * crash startup on the same chunk-mesh / entity-cull targets OBE reworks.
 *
 * <p>Sodium is intentionally NOT listed here: cushions are baked into Sodium chunk
 * meshes by {@code SodiumChunkMeshMixin} and committed by {@code LevelRendererMixin}.
 */
public class OptimizedCushionsMixinPlugin implements IMixinConfigPlugin {

    private boolean disabled;

    @Override
    public void onLoad(final String mixinPackage) {
        this.disabled = isObeLoaded();
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return !this.disabled;
    }

}