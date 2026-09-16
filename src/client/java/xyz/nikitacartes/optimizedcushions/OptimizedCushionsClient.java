package xyz.nikitacartes.optimizedcushions;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
//?} else {
/*import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
*///?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric {
public class OptimizedCushionsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("optimizedcushionsbackport");

    public static boolean isObeLoaded() {
        return FabricLoader.getInstance().isModLoaded("obe");
    }

    public static boolean isSodiumLoaded() {
        return FabricLoader.getInstance().isModLoaded("sodium");
    }

    @Override
    public void onInitializeClient() {
        if (isObeLoaded()) {
            LOGGER.info("Optimised Block Entities detected - Optimized Cushions Backport client is disabled, cushions render as backport entities.");
            return;
        }

        if (isSodiumLoaded()) {
            //? if >=1.21.1 {
            LOGGER.info("Sodium detected - baking cushions into Sodium chunk meshes.");
            //?} else {
            /*LOGGER.warn("Sodium detected - Optimized Cushions Backport client baking is disabled, cushions render as entities.");
            return;
            *///?}
        }

        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof OptCushion) {
                CushionTracker.onLoad(entity);
            }
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof OptCushion) {
                CushionTracker.onUnload(entity);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(CushionTracker::tick);
    }
}
//?} else {
/*@Mod(value = "optimizedcushionsbackport", dist = Dist.CLIENT)
public class OptimizedCushionsClient {
    public static final Logger LOGGER = LoggerFactory.getLogger("optimizedcushionsbackport");

    public OptimizedCushionsClient() {
        if (ModList.get().isLoaded("obe")) {
            LOGGER.info("Optimised Block Entities detected - Optimized Cushions Backport client is disabled, cushions render as backport entities.");
            return;
        }

        if (ModList.get().isLoaded("sodium")) {
            LOGGER.info("Sodium detected - baking cushions into Sodium chunk meshes.");
        }
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof OptCushion) {
                CushionTracker.onLoad(event.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof OptCushion) {
                CushionTracker.onUnload(event.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            CushionTracker.tick(Minecraft.getInstance());
        });
    }
}
*///?}
