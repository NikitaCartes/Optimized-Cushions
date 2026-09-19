package xyz.nikitacartes.optimizedcushions.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import xyz.nikitacartes.optimizedcushions.mixin.server.ServerLevelAccessor;
import xyz.nikitacartes.optimizedcushions.server.CushionServerExt;
import xyz.nikitacartes.optimizedcushions.server.ServerLevelExt;

/**
 * Server-side game tests for the cushion tick/tracker optimisations. Each test asserts the
 * white-box routing flags ({@link CushionServerExt}) and/or the observable pop/drop behaviour,
 * proving the {@code CushionServerTicker} stays equivalent to the backport entity's own tick.
 *
 * <p>Uses the default 8x8 empty structure; a stone floor is laid at y=1 so cushions have support.
 *
 * <p>The backport entity is looked up from the registry (no compile dependency on
 * Cushion-Backport); colour goes through {@link CushionServerExt}.
 */
public class CushionServerGameTests {
    private static final BlockPos CUSHION = new BlockPos(4, 2, 4);
    private static final BlockPos SUPPORT = new BlockPos(4, 1, 4);

    private static EntityType<Entity> cushionType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("cushionbackport", "cushion"));
        if (type == null) {
            throw new AssertionError("cushionbackport:cushion is not registered");
        }
        @SuppressWarnings("unchecked")
        EntityType<Entity> cast = (EntityType<Entity>) type;
        return cast;
    }

    // EntityTypes (plural holder) exists only on newer 26.x; the registry lookup works everywhere.
    private static EntityType<Mob> pigType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", "pig"));
        if (type == null) {
            throw new AssertionError("minecraft:pig is not registered");
        }
        @SuppressWarnings("unchecked")
        EntityType<Mob> cast = (EntityType<Mob>) type;
        return cast;
    }

    private static Item whiteCushionItem() {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("cushionbackport", "white_cushion"));
        if (item == null) {
            throw new AssertionError("cushionbackport:white_cushion is not registered");
        }
        return item;
    }

    // --- routing ---

    /** A passenger-free cushion is pulled out of the vanilla tick list into our ticker. */
    @GameTest
    public void passengerFreeCushionRoutedToTicker(GameTestHelper helper) {
        floor(helper);
        Entity cushion = helper.spawn(cushionType(), CUSHION);
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
        Entity cushion = helper.spawn(cushionType(), CUSHION);
        Mob pig = helper.spawn(pigType(), CUSHION);
        pig.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(inTicker(cushion), "starts in ticker");

            pig.startRiding(cushion, true, true);
            helper.assertFalse(inTicker(cushion), "ridden cushion must move to the vanilla tick list");

            pig.stopRiding();
            helper.assertTrue(inTicker(cushion), "idle cushion must be reclaimed by the ticker");
            assertSingleMembership(helper, cushion);
            helper.succeed();
        });
    }

    /** Demotion must not depend on the server-ticking flag: an idle cushion in the
     * post-stopTicking window (unticked, flag cleared) still converges back to the ticker. */
    @GameTest
    public void demoteWhileUntickedReclaims(GameTestHelper helper) {
        floor(helper);
        Entity cushion = helper.spawn(cushionType(), CUSHION);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(inTicker(cushion), "starts in ticker");
            var level = cushion.level();
            var tickList = ((ServerLevelAccessor) level).optimizedcushions$getEntityTickList();
            // Simulate the chunk-not-ticking window: on the vanilla list but flag cleared.
            ((ServerLevelExt) level).optimizedcushions$cushionTicker().remove(cushion);
            tickList.add(cushion);
            ((CushionServerExt) cushion).optimizedcushions$setServerTicking(false);
            ((ServerLevelExt) level).optimizedcushions$cushionTicker().demoteIfIdle(cushion);
            helper.assertTrue(inTicker(cushion), "idle cushion must be reclaimed even while unticked");
            assertSingleMembership(helper, cushion);
            helper.succeed();
        });
    }

    /** A cushion made to ride something (/ride) must leave the ticker so rideTick runs vanilla. */
    @GameTest
    public void cushionRidingVehicleLeavesTicker(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawn(pigType(), CUSHION);
        pig.setNoAi(true);
        Entity cushion = helper.spawn(cushionType(), CUSHION);
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
        Entity cushion = helper.spawn(cushionType(), CUSHION);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(inTicker(cushion), "starts in ticker");
            ((CushionServerExt) cushion).optimizedcushions$setColor(DyeColor.RED);
            helper.assertTrue(((CushionServerExt) cushion).optimizedcushions$color() == DyeColor.RED, "colour should update");
            helper.assertTrue(inTicker(cushion), "dye must not evict the cushion from the ticker");
            helper.succeed();
        });
    }

    // --- behaviour parity (pop / drop happen from inside the ticker) ---

    /** Removing the support ticks the cushion via our ticker, so it still pops and drops its item. */
    @GameTest(maxTicks = 220)
    public void popsAndDropsWhenSupportRemoved(GameTestHelper helper) {
        floor(helper);
        Entity cushion = helper.spawn(cushionType(), CUSHION);
        ((CushionServerExt) cushion).optimizedcushions$setColor(DyeColor.WHITE);
        helper.runAfterDelay(3, () -> {
            helper.assertEntityPresent(cushionType());
            helper.destroyBlock(SUPPORT);
        });
        helper.succeedWhen(() -> {
            helper.assertEntityNotPresent(cushionType());
            helper.assertItemEntityPresent(whiteCushionItem(), CUSHION, 3.0);
        });
    }

    /** A cushion with a rider lives in the vanilla list, yet still pops when its support is removed. */
    @GameTest(maxTicks = 220)
    public void riddenCushionStillPops(GameTestHelper helper) {
        floor(helper);
        Entity cushion = helper.spawn(cushionType(), CUSHION);
        Mob pig = helper.spawn(pigType(), CUSHION);
        pig.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            pig.startRiding(cushion, true, true);
            helper.destroyBlock(SUPPORT);
        });
        helper.succeedWhen(() -> {
            helper.assertEntityNotPresent(cushionType());
            helper.assertEntityPresent(pigType());
        });
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private static boolean inTicker(Entity cushion) {
        return ((CushionServerExt) cushion).optimizedcushions$isInTicker();
    }

    private static boolean serverTicking(Entity cushion) {
        return ((CushionServerExt) cushion).optimizedcushions$isServerTicking();
    }

    private static void assertSingleMembership(GameTestHelper helper, Entity cushion) {
        boolean inList = ((ServerLevelAccessor) cushion.level()).optimizedcushions$getEntityTickList().contains(cushion);
        helper.assertTrue(inList != inTicker(cushion), "cushion must live in exactly one tick list");
    }
}
