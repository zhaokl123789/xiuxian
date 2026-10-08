package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import xiuxian.block.OrientalBlocks;

@GameTestHolder("xiuxian_town")
@PrefixGameTestTemplate(false)
public final class MortalTownGameTests {
    // The headless test server runs uncapped ticks while cold chunks load asynchronously.
    @GameTest(templateNamespace = "xiuxian_town", template = "empty", timeoutTicks = 36000,
            batch = "mortal_town_full")
    public static void fullTownBuildsAcrossColdChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        MortalTownConstruction.cancel(level);
        BlockPos origin = new BlockPos(1024, 32, 1024);
        BlockPos obstacle = origin.offset(172, 180, 128);
        BlockPos outside = origin.offset(173, 180, 128);
        level.setBlock(obstacle, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(outside, Blocks.STONE.defaultBlockState(), 2);
        BlockPos doorway = origin.offset(0, 3, -108);
        level.setBlock(doorway, Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(MortalTownGenerator.generate(level, origin), "Full town failed to queue");
        MortalTownConstruction.Data data = MortalTownConstruction.data(level);
        data.version = 3;
        data.building = false;
        helper.succeedWhen(() -> {
            helper.assertTrue(data.origin == null && data.contains(origin),
                    "Full town incomplete: " + MortalTownConstruction.status(level));
            helper.assertTrue(level.getBlockState(obstacle).isAir(), "High site boundary was not cleared");
            helper.assertTrue(level.getBlockState(outside).is(Blocks.STONE), "Full clearing exceeded boundary");
            helper.assertTrue(level.getBlockState(origin.offset(172, 0, 128))
                    .equals(OrientalBlocks.state("town_black_bricks")), "Outer foundation missing");
            helper.assertTrue(level.getBlockState(origin.offset(0, 2, -8))
                    .equals(OrientalBlocks.state("town_throne")), "Palace furnishing missing");
            helper.assertTrue(level.getBlockState(origin.offset(-7, 2, -7))
                    .equals(OrientalBlocks.state("town_low_desk")), "Palace desk missing");
            helper.assertTrue(level.getBlockState(origin.offset(0, 3, -108)).isAir(), "City gate blocked");
            helper.assertTrue(level.getBlockState(origin.offset(-22, 2, -16))
                    .equals(OrientalBlocks.state("xian_chime_rack")), "Palace ritual chimes erased");
            helper.assertTrue(level.getBlockState(origin.offset(-58, 3, 91))
                    .equals(OrientalBlocks.state("xian_apothecary_drawers")), "Medicine shop stock erased");
            helper.assertTrue(level.getBlockState(origin.offset(-50, 3, 88))
                    .equals(OrientalBlocks.state("xian_alchemy_furnace")), "Medicine shop furnace erased");
            helper.assertTrue(level.getBlockState(origin.offset(-57, 4, 92)).getLightEmission() == 15,
                    "Medicine shop wall lamp missing or dark");
            boolean residentialDoor = false;
            boolean residentialBed = false;
            boolean residentialLight = false;
            for (int x = -149; x <= -80; x++) {
                for (int z = -106; z <= -34; z++) {
                    if (level.getBlockState(origin.offset(x, 2, z)).is(Blocks.SPRUCE_DOOR)) {
                        residentialDoor = true;
                    }
                    if (level.getBlockState(origin.offset(x, 2, z)).is(Blocks.RED_BED)) {
                        residentialBed = true;
                    }
                    if (level.getBlockState(origin.offset(x, 6, z)).getLightEmission() == 15) {
                        residentialLight = true;
                    }
                }
            }
            helper.assertTrue(residentialDoor, "Residential house entrance door missing");
            helper.assertTrue(residentialLight, "Residential house interior remains dark");
            helper.assertTrue(residentialBed, "Residential sleeping corner missing");
            helper.assertTrue(level.getBlockState(origin.offset(-96, 3, -80)).is(Blocks.SPRUCE_DOOR),
                    "Market shop entrance door missing");
            helper.assertTrue(level.getBlockState(origin.offset(-96, 6, -72)).getLightEmission() == 15,
                    "Market shop interior remains dark");
            helper.assertTrue(level.getBlockState(origin.offset(0, 2, -27)).is(Blocks.DARK_OAK_DOOR),
                    "Central palace entrance door missing");
            helper.assertTrue(level.getBlockState(origin.offset(-11, 2, -104)).is(Blocks.SPRUCE_DOOR),
                    "North gate tower entrance door missing");
            helper.assertTrue(level.getBlockState(origin.offset(-150, 2, -102)).is(Blocks.SPRUCE_DOOR),
                    "Corner watchtower entrance door missing");
            helper.assertTrue(level.getBlockState(origin.offset(-102, 2, 0)).is(Blocks.SPRUCE_DOOR),
                    "Pagoda entrance door missing");
            helper.assertTrue(level.getBlockState(origin.offset(-112, 6, 0)).getLightEmission() == 15,
                    "Pagoda or tower interior lamp missing");
            helper.assertTrue(!MortalTownConstruction.JOBS.containsKey(level), "Completed job still holds chunk ticket");
        });
    }

    @GameTest(templateNamespace = "xiuxian_town", template = "empty", timeoutTicks = 1800,
            batch = "mortal_town")
    public static void clearingPreservesOutsideAndResumesWithoutFreezing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        MortalTownConstruction.cancel(level);
        // Check the production footprint reaches every outer-house eave, not just the city wall.
        var real = MortalTownGenerator.createPlan();
        helper.assertTrue(real.minX <= -169 && real.maxX >= 169 && real.minZ <= -124 && real.maxZ >= 124,
                "Clearing misses outlying houses or gate roofs");
        BlockPos origin = new BlockPos(-1008, 32, -1008);
        BlockPos edge = origin.offset(17, 170, 17), inside = origin.offset(0, 3, 0);
        BlockPos outside = origin.offset(18, 170, 17), deep = origin.offset(0, -3, 0);
        for (BlockPos obstacle : new BlockPos[]{edge, outside, deep})
            level.setBlock(obstacle, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(inside, Blocks.CHEST.defaultBlockState(), 2);
        ((ChestBlockEntity) level.getBlockEntity(inside)).setItem(0, new ItemStack(Items.DIAMOND, 64));
        var plan = new MortalTownGenerator.Plan();
        plan.add(-17,-2,-17,17,0,17,OrientalBlocks.state("town_black_bricks"));
        plan.add(-2,1,-2,2,4,-2,OrientalBlocks.state("town_white_plaster"));
        plan.add(0,1,-2,0,3,-2,Blocks.AIR.defaultBlockState()); // ordered doorway cut
        BlockPos doorway = origin.offset(0, 2, -2);
        level.setBlock(doorway, Blocks.CHEST.defaultBlockState(), 2);
        var blockedChest = (ChestBlockEntity) level.getBlockEntity(doorway);
        blockedChest.setItem(0, new ItemStack(Items.DIAMOND, 64));
        BlockState desk = OrientalBlocks.state("town_low_desk")
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        plan.add(1,1,0,1,1,0,desk);
        plan.add(-1,1,0,-1,1,0,OrientalBlocks.state("town_red_lantern"));
        helper.assertTrue(MortalTownConstruction.start(level, origin, plan), "Failed to queue town");
        helper.assertTrue(level.getBlockState(inside).is(Blocks.CHEST),
                "Starting a build performed synchronous world writes");
        helper.assertTrue(!MortalTownConstruction.start(level, origin.offset(300,0,0), plan),
                "Second activation replaced the running job");
        MortalTownConstruction.Data data = MortalTownConstruction.data(level);
        // Reload the cursor and release its ticket while preserving the fixture blueprint.
        helper.runAtTickTime(8, () -> {
            var restored = MortalTownConstruction.Data.load(data.save(new CompoundTag()));
            helper.assertTrue(restored.origin.equals(origin) && restored.cell == data.cell
                    && restored.chunk == data.chunk && restored.building == data.building,
                    "Saved cursor failed to round trip");
            MortalTownConstruction.release(level);
            data.origin = restored.origin; data.cell = restored.cell;
            data.chunk = restored.chunk; data.operation = restored.operation;
            // Production reload recreates the same blueprint; the test substitutes its tiny fixture.
            MortalTownConstruction.start(level, origin, plan); // must be rejected as still active
            MortalTownConstruction.JOBS.put(level, new MortalTownConstruction.Job(origin, plan));
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(data.origin == null && data.contains(origin), "Construction still running");
            helper.assertTrue(level.getBlockState(edge).isAir(), "High site edge was not cleared");
            helper.assertTrue(level.getBlockState(inside).isAir(), "Chest inside the site was not cleared");
            helper.assertTrue(blockedChest.isEmpty() && level.getBlockEntity(doorway) == null,
                    "Doorway obstacle inventory was not removed");
            helper.assertTrue(level.getBlockState(outside).is(Blocks.STONE), "Outside obstacle was removed");
            helper.assertTrue(level.getBlockState(deep).is(Blocks.STONE), "Deep underground was cleared");
            helper.assertTrue(level.getBlockState(origin.offset(0,0,0)).equals(OrientalBlocks.state("town_black_bricks")),
                    "Foundation was not built after clearing");
            helper.assertTrue(level.getBlockState(origin.offset(0,2,-2)).isAir(), "Doorway ordering broke");
            helper.assertTrue(level.getBlockState(origin.offset(1,1,0)).equals(desk), "Furniture orientation lost");
            helper.assertTrue(level.getBlockState(origin.offset(-1,1,0)).getLightEmission() == 15,
                    "New lantern does not emit light");
            helper.assertTrue(!MortalTownConstruction.start(level, origin, plan), "Completed origin was accepted twice");
            var legacy = new CompoundTag(); legacy.putLongArray("Origins", new long[]{origin.asLong()});
            helper.assertTrue(MortalTownConstruction.Data.load(legacy).contains(origin), "Old town records lost");
        });
    }
}
