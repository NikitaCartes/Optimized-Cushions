package xyz.nikitacartes.optimizedcushions;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.decoration.Cushion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizedCushionsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("optimizedcushions");

    public static boolean isObeLoaded() {
        return FabricLoader.getInstance().isModLoaded("obe");
    }

    private static boolean isSodiumLoaded() {
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
