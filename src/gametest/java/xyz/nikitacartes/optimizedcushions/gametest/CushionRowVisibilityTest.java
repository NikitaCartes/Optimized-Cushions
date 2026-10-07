package xyz.nikitacartes.optimizedcushions.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import xyz.nikitacartes.optimizedcushions.CushionTracker;

/**
 * Row visibility test: cushions in a row along +X starting at the player, inside the default
 * gametest view window (render/view distance 5, so everything below also holds at larger distances
 * up to the vanilla 160-block cushion tracking range).
 *
 * <p>Mod enabled (baked): every cushion ends up {@link CushionTracker#isBaked}, so the whole row
 * renders like terrain. Vanilla proxy (glowing forces the entity path, same as mod disabled):
 * nothing is baked, so the far end culls at entity distance (halved here for contrast).
 * Both states are screenshotted.
 */
public class CushionRowVisibilityTest implements FabricClientGameTest {
    // Fits the default 5-chunk window with margin; far ends sit past the halved entity
    // cull distance (~32 blocks) from a side-on camera. Kept at 64: larger simultaneous
    // glow herds unbaking timing goes nondeterministic (10s to 60s+).
    private static final int COUNT = 64;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            var server = singleplayer.getServer();
            // Entity distance is client-side only (no chunk churn); halving it sharpens the vanilla contrast.
            // Hidden chat keeps command feedback out of the screenshots.
            context.runOnClient(client -> {
                client.options.entityDistanceScaling().set(0.5);
                client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
            });
            singleplayer.getConnection().waitForChunksRender();

            // Anchor the row to the player spawn: every block is already loaded/ticketed.
            BlockPos base = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
            int floorY = base.getY() - 1;
            int z = base.getZ();
            int x0 = base.getX();
            server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x0 - 2, floorY, z - 5, x0 + COUNT + 2, floorY, z + 5));
            for (int i = 0; i < COUNT; i++) {
                server.runCommand("summon minecraft:cushion %s %d %s".formatted(x0 + i + 0.5, floorY + 1, z + 0.5));
            }
            // Side-on camera south of the row middle, looking north and down at the row.
            server.runCommand("tp @a %s %s %s 180 20".formatted(x0 + COUNT / 2 + 0.5, floorY + 3, z + 6.5));
            singleplayer.getConnection().waitForChunksRender();

            // Mod path: whole row baked into chunk meshes.
            context.waitFor(client -> countBaked(client) == COUNT, 1200);
            context.takeScreenshot("cushions_row_baked");

            // Vanilla proxy: glowing forces the entity path (no baking), far end culls at entity distance.
            server.runCommand("execute as @e[type=minecraft:cushion] run data merge entity @s {Glowing:1b}");
            context.waitFor(client -> countBaked(client) == 0, 1200);
            context.takeScreenshot("cushions_row_vanilla");
        }
    }

    private static int countBaked(Minecraft client) {
        int baked = 0;
        for (Cushion cushion : cushions(client)) {
            if (CushionTracker.isBaked(cushion)) {
                baked++;
            }
        }
        return baked;
    }

    private static List<Cushion> cushions(Minecraft client) {
        List<Cushion> out = new ArrayList<>();
        if (client.level == null) {
            return out;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Cushion cushion) {
                out.add(cushion);
            }
        }
        return out;
    }
}
