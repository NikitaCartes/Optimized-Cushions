package xyz.nikitacartes.optimizedcushions.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Skips all client mixins when a Sodium-like renderer is present. The runtime
 * guard in {@code OptimizedCushionsClient} alone is not enough: with
 * {@code required:true} the mixins would still apply and can crash startup on
 * the same chunk-mesh / entity-cull targets Sodium reworks.
 */
public class OptimizedCushionsMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> OPTIMIZATION_MODS = Set.of(
            "sodium", "obe"
    );

    private boolean sodiumLikeLoaded;

    @Override
    public void onLoad(final String mixinPackage) {        var loader = FabricLoader.getInstance();
        this.sodiumLikeLoaded = OPTIMIZATION_MODS.stream().anyMatch(loader::isModLoaded);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return !this.sodiumLikeLoaded;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
    }
}
