package xyz.nikitacartes.optimizedcushions.mixin;

//? if fabric {
import static xyz.nikitacartes.optimizedcushions.OptimizedCushionsClient.isObeLoaded;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
*///?}
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides whether the client mixins apply.
 *
 * <p>Fabric: skips everything when OBE is present (OBE bakes cushions itself; running
 * both would double-bake). {@code FabricLoader} is safe to read this early, and
 * dedicated servers never reach here ({@code "environment": "client"}).
 *
 * <p>NeoForge: skips everything on a dedicated server, where the targets (vanilla
 * client classes) do not exist and {@code required:true} would crash startup
 * (NeoForge's {@code [[mixins]]} has no environment scoping). Uses the launch dist,
 * which — unlike {@code ModList}, still null this early — is safe to read. OBE is
 * handled at runtime in {@code OptimizedCushionsClient} instead: our injections
 * compose with OBE's (different methods, no removed vanilla targets).
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
        this.disabled = isObeLoaded();
        //?} else {
        /*this.disabled = FMLEnvironment.getDist() == Dist.DEDICATED_SERVER;
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
