package xyz.nikitacartes.optimizedcushions.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides whether the client mixins apply.
 *
 * <p>Fabric: nothing to skip (dedicated servers never reach here
 * ({@code "environment": "client"}), and OBE does not bake backport cushions).
 *
 * <p>NeoForge: skips everything on a dedicated server, where the targets (vanilla
 * client classes) do not exist and {@code required:true} would crash startup
 * (NeoForge's {@code [[mixins]]} has no environment scoping). Probes for the
 * client class resource directly, which — unlike {@code ModList}, still null this early —
 * is safe to read. Uses {@code getResource}, never {@code Class.forName}: loading
 * {@code Minecraft} here marks it "already loaded" for Mixin and breaks Sodium's
 * {@code MinecraftMixin} with {@code MixinTargetAlreadyLoadedException}.
 *
 * <p>Sodium is intentionally NOT listed on either loader: cushions are baked into
 * Sodium chunk meshes by {@code SodiumChunkMeshMixin} and committed by
 * {@code LevelRendererMixin}.
 */
public class OptimizedCushionsMixinPlugin implements IMixinConfigPlugin {

    private boolean disabled;

    @Override
    public void onLoad(final String mixinPackage) {
        //? if fabric {
        //?} else {
        /*try {
            this.disabled = getClass().getClassLoader().getResource("net/minecraft/client/Minecraft.class") == null;
        } catch (Throwable t) {
            this.disabled = true;
        }
        *///?}
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return !this.disabled;
    }

    // Remaining IMixinConfigPlugin methods: defaults. (NeoForge's Mixin still declares them
    // abstract; Fabric's has defaults, where these are plain overrides.)
    @Override
    public String getRefMapperConfig() {
        return null;
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
