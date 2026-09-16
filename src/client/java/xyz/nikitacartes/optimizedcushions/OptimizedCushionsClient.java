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
import net.minecraft.world.entity.decoration.Cushion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric {
public class OptimizedCushionsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("optimizedcushions");

    public static boolean isObeLoaded() {
        return FabricLoader.getInstance().isModLoaded("obe");
    }

    public static boolean isSodiumLoaded() {
        return FabricLoader.getInstance().isModLoaded("sodium");
    }

    @Override
    public void onInitializeClient() {
        if (isObeLoaded()) {
            LOGGER.info("Optimised Block Entities detected - Optimized Cushions client is disabled, cushions render as vanilla entities.");
            return;
        }

        if (isSodiumLoaded()) {
            LOGGER.info("Sodium detected - baking cushions into Sodium chunk meshes.");
        }

        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Cushion cushion) {
                CushionTracker.onLoad(cushion);
            }
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Cushion cushion) {
                CushionTracker.onUnload(cushion);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(CushionTracker::tick);
    }
}
//?} else {
/*@Mod(value = "optimizedcushions", dist = Dist.CLIENT)
public class OptimizedCushionsClient {
    public static final Logger LOGGER = LoggerFactory.getLogger("optimizedcushions");

    // @Mod is Dist.CLIENT so this never loads on a dedicated server.
    public OptimizedCushionsClient() {
        if (ModList.get().isLoaded("obe")) {
            LOGGER.info("Optimised Block Entities detected - Optimized Cushions client is disabled, cushions render as vanilla entities.");
            return;
        }

        if (ModList.get().isLoaded("sodium")) {
            LOGGER.info("Sodium detected - baking cushions into Sodium chunk meshes.");
        }

        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof Cushion cushion) {
                CushionTracker.onLoad(cushion);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof Cushion cushion) {
                CushionTracker.onUnload(cushion);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            CushionTracker.tick(Minecraft.getInstance());
        });
    }
}
*///?}
