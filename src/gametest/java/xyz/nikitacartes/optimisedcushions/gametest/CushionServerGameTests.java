package xyz.nikitacartes.optimisedcushions.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import xyz.nikitacartes.optimisedcushions.server.CushionServerExt;

/**
 * Server-side game tests for the cushion tick/tracker optimisations. Each test asserts the
 * white-box routing flags ({@link CushionServerExt}) and/or the observable pop/drop behaviour,
 * proving the {@code CushionServerTicker} stays vanilla-equivalent.
 *
 * <p>Uses the default 8x8 empty structure; a stone floor is laid at y=1 so cushions have support.
 */
public class CushionServerGameTests {
    private static final BlockPos CUSHION = new BlockPos(4, 2, 4);
    private static final BlockPos SUPPORT = new BlockPos(4, 1, 4);

    // --- routing ---

    /** A passenger-free cushion is pulled out of the vanilla tick list into our ticker. */
    @GameTest
    public void passengerFreeCushionRoutedToTicker(GameTestHelper helper) {
        floor(helper);
        Cushion cushion = helper.spawn(EntityTypes.CUSHION, CUSHION);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(serverTicking(cushion), "cushion should be server-ticking");
            helper.assertTrue(inTicker(cushion), "passenger-free cushion should live in the ticker");
            helper.succeed();
        });
    }

    /** Sitting on a cushion promotes it to the vanilla list; getting up demotes it back. */
    @GameTest
    public void ridePromotesAndDismountDemotes(GameTestHelper helper) {
        floor(helper);
        Cushion cushion = helper.spawn(EntityTypes.CUSHION, CUSHION);
        Mob pig = helper.spawn(EntityTypes.PIG, CUSHION);
        pig.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(inTicker(cushion), "starts in ticker");

            pig.startRiding(cushion, true, true);
            helper.assertFalse(inTicker(cushion), "ridden cushion must move to the vanilla tick list");

            pig.stopRiding();
            helper.assertTrue(inTicker(cushion), "idle cushion must be reclaimed by the ticker");
            helper.succeed();
        });
    }

    /** A cushion made to ride something (/ride) must leave the ticker so rideTick runs vanilla. */
    @GameTest
    public void cushionRidingVehicleLeavesTicker(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawn(EntityTypes.PIG, CUSHION);
        pig.setNoAi(true);
        Cushion cushion = helper.spawn(EntityTypes.CUSHION, CUSHION);
        helper.runAfterDelay(3, () -> {
            cushion.startRiding(pig, true, true);
            helper.assertTrue(cushion.isPassenger(), "cushion should be riding the pig");
            helper.assertFalse(inTicker(cushion), "cushion riding a vehicle must not be in the ticker");
            helper.succeed();
        });
    }

    /** Dyeing a quiescent, ticker-owned cushion applies and does not evict it from the ticker. */
    @GameTest
    public void colourAppliesWhileTracked(GameTestHelper helper) {
        floor(helper);
        Cushion cushion = helper.spawn(EntityTypes.CUSHION, CUSHION);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(inTicker(cushion), "starts in ticker");
            cushion.setColor(DyeColor.RED);
            helper.assertTrue(cushion.getColor() == DyeColor.RED, "colour should update");
            helper.assertTrue(inTicker(cushion), "dye must not evict the cushion from the ticker");
            helper.succeed();
        });
    }

    // --- behaviour parity (pop / drop happen from inside the ticker) ---

    /** Removing the support ticks the cushion via our ticker, so it still pops and drops its item. */
    @GameTest(maxTicks = 220)
    public void popsAndDropsWhenSupportRemoved(GameTestHelper helper) {
        floor(helper);
        helper.spawn(EntityTypes.CUSHION, CUSHION);
        helper.runAfterDelay(3, () -> {
            helper.assertEntityPresent(EntityTypes.CUSHION);
            helper.destroyBlock(SUPPORT);
        });
        helper.succeedWhen(() -> {
            helper.assertEntityNotPresent(EntityTypes.CUSHION);
            helper.assertItemEntityPresent(Items.CUSHION.pick(DyeColor.WHITE), CUSHION, 3.0);
        });
    }

    /** A cushion with a rider lives in the vanilla list, yet still pops when its support is removed. */
    @GameTest(maxTicks = 220)
    public void riddenCushionStillPops(GameTestHelper helper) {
        floor(helper);
        Cushion cushion = helper.spawn(EntityTypes.CUSHION, CUSHION);
        Mob pig = helper.spawn(EntityTypes.PIG, CUSHION);
        pig.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            pig.startRiding(cushion, true, true);
            helper.destroyBlock(SUPPORT);
        });
        helper.succeedWhen(() -> {
            helper.assertEntityNotPresent(EntityTypes.CUSHION);
            helper.assertEntityPresent(EntityTypes.PIG);
        });
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private static boolean inTicker(Cushion cushion) {
        return ((CushionServerExt) cushion).optimisedcushions$isInTicker();
    }

    private static boolean serverTicking(Cushion cushion) {
        return ((CushionServerExt) cushion).optimisedcushions$isServerTicking();
    }
}
