package xyz.nikitacartes.optimisedcushions;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.decoration.Cushion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimisedCushionsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("optimisedcushions");

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("sodium")) {
            LOGGER.warn("Sodium detected - Optimised Cushions is disabled, cushions render as vanilla entities.");
            return;
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
