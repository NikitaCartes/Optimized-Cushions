package xyz.nikitacartes.optimizedcushions.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides which mixins apply.
 *
 * <p>NeoForge: skips client mixins on a dedicated server (no environment scoping;
 * {@code required:true} would crash). Probes {@code Minecraft.class} via {@code getResource},
 * never {@code Class.forName} (breaks Sodium's {@code MinecraftMixin}).
 *
 * <p>Fabric client config never reaches a dedicated server; Sodium is baked into, not skipped.
 *
 * <p>Per-mod mixins apply only when that mod is present (resource probe); vanilla targets always apply.
 */
public class OptimizedCushionsMixinPlugin implements IMixinConfigPlugin {
    private static final String CB_CUSHION = "com/leclowndu93150/cushionbackport/entity/Cushion.class";
    private static final String CB_RENDERER = "com/leclowndu93150/cushionbackport/client/CushionRenderer.class";
    private static final String VB_CUSHION = "com/blackgear/vanillabackport/common/level/entities/decoration/Cushion.class";
    private static final String VB_RENDERER = "com/blackgear/vanillabackport/client/level/renderer/object/CushionRenderer.class";

    private static final Map<String, String> TARGET_BY_MIXIN = Map.ofEntries(
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.CushionMixin", CB_CUSHION),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.server.CushionMixin", CB_CUSHION),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.CushionRendererMixin", CB_RENDERER),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.CushionRendererOldMixin", CB_RENDERER),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.CushionRendererLegacyMixin", CB_RENDERER),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.CushionRenderStateMixin", "com/leclowndu93150/cushionbackport/client/CushionRenderState.class"),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.VanillaCushionMixin", VB_CUSHION),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.server.VanillaCushionMixin", VB_CUSHION),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.VanillaCushionRendererOldMixin", VB_RENDERER),
        Map.entry("xyz.nikitacartes.optimizedcushions.mixin.VanillaCushionRendererLegacyMixin", VB_RENDERER)
    );

    private final Map<String, Boolean> targetPresent = new ConcurrentHashMap<>();

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
        if (this.disabled) {
            return false;
        }
        String target = TARGET_BY_MIXIN.get(mixinClassName);
        if (target == null) {
            // Vanilla target: always applies.
            return true;
        }
        return this.targetPresent.computeIfAbsent(target, resource -> getClass().getClassLoader().getResource(resource) != null);
    }

    // Defaults (abstract on NeoForge, defaulted on Fabric).
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
