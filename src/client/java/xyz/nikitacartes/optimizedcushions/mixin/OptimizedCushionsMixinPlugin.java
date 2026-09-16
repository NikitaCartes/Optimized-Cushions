package xyz.nikitacartes.optimizedcushions.mixin;

//? if fabric {
import static xyz.nikitacartes.optimizedcushions.OptimizedCushionsClient.isObeLoaded;
//?}
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class OptimizedCushionsMixinPlugin implements IMixinConfigPlugin {

    private boolean disabled;

    @Override
    public void onLoad(final String mixinPackage) {
        //? if fabric {
        this.disabled = isObeLoaded();
        //?} else {
        /*try {
            Class.forName("net.minecraft.client.Minecraft", false, getClass().getClassLoader());
            this.disabled = false;
        } catch (Throwable t) {
            this.disabled = true;
        }
        *///?}
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return !this.disabled;
    }

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
