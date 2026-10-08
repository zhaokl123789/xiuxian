package xiuxian.sect;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Blueprint-only acceptance checks for the standalone Dao-Tai residence. */
@GameTestHolder("xiuxian_daotai")
@PrefixGameTestTemplate(false)
public final class DaotaiResidenceGameTests {
    private DaotaiResidenceGameTests() {}

    @GameTest(templateNamespace = "xiuxian_geometry", template = "empty", timeoutTicks = 72000,
            batch = "daotai_full_build")
    public static void fullResidenceBuildsAcrossColdChunks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().overworld();
        var origin = new net.minecraft.core.BlockPos(4096, 40, 4096);
        var edge = origin.offset(220, 210, 210);
        var outside = edge.east();
        level.setBlock(edge, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(outside, Blocks.STONE.defaultBlockState(), 2);
        var top = origin.offset(-220, level.getMaxBuildHeight() - 1 - origin.getY(), -210);
        level.setBlock(top, Blocks.STONE.defaultBlockState(), 2);
        var doorway = origin.offset(0, 76, -62);
        var gap = origin.offset(200, 100, 200);
        level.setBlock(doorway, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(gap, Blocks.CHEST.defaultBlockState(), 2);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(gap))
                .setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 64));
        helper.assertTrue(DaotaiResidenceGenerator.generate(level, origin), "Full residence failed to queue");
        var data = DaotaiResidenceConstruction.data(level);
        data.phase = DaotaiResidenceConstruction.Phase.CLEAR;
        helper.succeedWhen(() -> {
            helper.assertTrue(data.problem.isEmpty(), "Construction paused: " + data.problem);
            helper.assertTrue(data.origin == null && data.contains(origin),
                    "Full residence incomplete: " + DaotaiResidenceConstruction.status(level));
            helper.assertTrue(level.getBlockState(edge).isAir(), "Site boundary was not cleared");
            helper.assertTrue(level.getBlockState(top).isAir(), "World-height corner was not cleared");
            helper.assertTrue(level.getBlockState(gap).isAir() && level.getBlockEntity(gap) == null,
                    "Chest between buildings was not cleared");
            helper.assertTrue(level.getBlockState(outside).is(Blocks.STONE), "Site clearing exceeded footprint");
            for (int[] point : new int[][]{{182,28,20},{134,48,20},{86,66,20},{80,74,-58},{54,113,-92},
                    {34,143,-92},{48,160,-150},{0,222,-150}}) {
                var actual = level.getBlockState(origin.offset(point[0], point[1], point[2]));
                helper.assertTrue(!actual.isAir(), "Cold-chunk construction missed landmark: " + java.util.Arrays.toString(point));
            }
            helper.assertTrue(level.getBlockState(origin.offset(-19,74,-97)).getLightEmission() == 15,
                    "Main palace floor light missing");
            helper.assertTrue(level.getBlockState(origin.offset(-22,75,-107)).getBlock()
                    .builtInRegistryHolder().key().location().getPath().startsWith("daotai_furniture_"),
                    "Main palace furnishing missing");
            helper.assertTrue(level.getBlockState(origin.offset(0,76,-62)).isAir(), "Palace entrance obstructed");
            helper.assertTrue(level.getBlockState(origin.offset(54,115,-92)).isAir(), "Upper landing obstructed");
            System.out.println("Dao-Tai FULL BUILD PASS: cold chunks, bounded clearing, landmarks, interior, upper landing");
        });
    }

    @GameTest(templateNamespace = "xiuxian_geometry", template = "empty", timeoutTicks = 400)
    public static void blueprintHasEstateScaleAndLandmarkHierarchy(GameTestHelper helper) throws java.io.IOException {
        DaotaiResidenceGenerator.Plan plan = DaotaiResidenceGenerator.createPlan();
        helper.assertTrue(plan.minX == DaotaiResidenceGenerator.MIN_X
                        && plan.maxX == DaotaiResidenceGenerator.MAX_X
                        && plan.minZ == DaotaiResidenceGenerator.MIN_Z
                        && plan.maxZ == DaotaiResidenceGenerator.MAX_Z,
                "Dao-Tai plan footprint is not the documented 441 x 421 estate");
        helper.assertTrue(plan.minY == DaotaiResidenceGenerator.MIN_Y
                        && plan.maxY == DaotaiResidenceGenerator.MAX_Y,
                "Dao-Tai plan height range changed unexpectedly");

        helper.assertTrue(stateAt(plan, 44, 76, -100).equals(Blocks.QUARTZ_BRICKS.defaultBlockState()),
                "Central Dao Palace wall is missing");
        helper.assertTrue(!stateAt(plan, 10, 8, 205).isAir(), "Southern gate mass is missing");
        helper.assertTrue(stateAt(plan, 0, 7, 205).isAir(),
                "Southern gate passage is blocked");
        helper.assertTrue(!stateAt(plan, -199, 4, -30).isAir(), "Western sky pagoda is missing");
        helper.assertTrue(!stateAt(plan, 199, 4, -30).isAir(), "Eastern sky pagoda is missing");
        helper.assertTrue(!stateAt(plan, 182, 28, 20).isAir(), "Outer cloud court is missing");
        helper.assertTrue(!stateAt(plan, 134, 48, 20).isAir(), "Middle cloud court is missing");
        helper.assertTrue(!stateAt(plan, 86, 66, 20).isAir(), "Inner cloud court is missing");
        helper.assertTrue(!stateAt(plan, 0, 220, -150).isAir(), "Central halo crown is missing");
        helper.assertTrue(!stateAt(plan, 80, 74, -58).isAir(), "Cardinal bridge is missing");
        helper.assertTrue(stateAt(plan, -19, 74, -97).getLightEmission() > 0,
                "Central palace floor lacks built-in lighting");

        int lampCount = 0, furnitureCount = 0, formationCount = 0, gardenCount = 0;
        for (DaotaiResidenceGenerator.Placement placement : plan.placements) {
            String path = placement.state().getBlock().builtInRegistryHolder().key().location().getPath();
            if (path.startsWith("daotai_lamp_")) {
                lampCount++;
                helper.assertTrue(placement.state().getLightEmission() == 15,
                        "Dao-Tai lamp lost full light emission: " + path);
            } else if (path.startsWith("daotai_furniture_")) furnitureCount++;
            else if (path.startsWith("daotai_formation_")) formationCount++;
            else if (path.startsWith("daotai_garden_")) gardenCount++;
        }
        helper.assertTrue(lampCount >= 100 && furnitureCount >= 40 && formationCount >= 30 && gardenCount >= 30,
                "Dao-Tai decoration density is too low: lamps=" + lampCount + ", furniture="
                        + furnitureCount + ", formations=" + formationCount + ", gardens=" + gardenCount);
        DaotaiGeometryVerification.verify(plan);
        helper.succeed();
    }

    private static BlockState stateAt(DaotaiResidenceGenerator.Plan plan, int x, int y, int z) {
        BlockState result = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (DaotaiResidenceGenerator.Placement placement : plan.placements) {
            if (x >= placement.minX() && x <= placement.maxX()
                    && y >= placement.minY() && y <= placement.maxY()
                    && z >= placement.minZ() && z <= placement.maxZ()) result = placement.state();
        }
        return result;
    }
}
