package xyz.nikitacartes.optimizedcushions.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

/**
 * Client game test for the section-baking half: proves a static cushion is baked into chunk
 * geometry, and that making it glow flips it back to the vanilla entity render path.
 */
public class CushionClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            singleplayer.getClientLevel().waitForChunksRender();

            BlockPos support = context.computeOnClient(client -> BlockPos.containing(client.player.position()).above(3).north(3));
            singleplayer.getServer().runCommand("setblock %d %d %d minecraft:stone".formatted(support.getX(), support.getY(), support.getZ()));
            singleplayer.getServer().runCommand("summon minecraft:cushion %s %d %s".formatted(support.getX() + 0.5, support.getY() + 1, support.getZ() + 0.5));

            // A plain cushion on a solid block must be baked into the section mesh.
            context.waitFor(client -> {
                Cushion cushion = findCushion(client);
                return cushion != null && CushionTracker.isBaked(cushion);
            });

            // Glowing forces the vanilla entity render path, so the cushion must stop being baked.
            singleplayer.getServer().runCommand("data merge entity @e[type=minecraft:cushion,limit=1] {Glowing:1b}");
            context.waitFor(client -> {
                Cushion cushion = findCushion(client);
                return cushion != null && !CushionTracker.isBaked(cushion);
            });
        }
    }

    private static Cushion findCushion(Minecraft client) {
        if (client.level == null) {
            return null;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Cushion cushion) {
                return cushion;
            }
        }
        return null;
    }
}
