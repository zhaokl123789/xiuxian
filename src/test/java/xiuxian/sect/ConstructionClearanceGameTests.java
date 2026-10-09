package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_clearance")
@PrefixGameTestTemplate(false)
public final class ConstructionClearanceGameTests {
    @GameTest(templateNamespace = "xiuxian_clearance", template = "empty", timeoutTicks = 400)
    public static void topDownClearanceCoversEveryClippedCellAndResumes(GameTestHelper helper) {
        var level = helper.getLevel().getServer().overworld();
        int minX = -9073, maxX = -9054, minZ = -9073, maxZ = -9054;
        int minY = 289, maxY = level.getMaxBuildHeight() - 1;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            for (int y = minY; y <= maxY; y++) level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        var outside = new BlockPos(minX - 1, maxY, minZ);
        var below = new BlockPos(minX, minY - 1, minZ);
        level.setBlock(outside, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        level.setBlock(below, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        for (int z = minZ >> 4; z <= maxZ >> 4; z++) for (int x = minX >> 4; x <= maxX >> 4; x++) {
            var chunk = level.getChunk(x, z);
            long cursor = 0;
            while (true) {
                var result = SiteClearance.clear(level, chunk, minX, minY, minZ, maxX, maxY, maxZ,
                        cursor, 137, 73, 8_000_000L);
                helper.assertTrue(result.problem().isEmpty(), result.problem());
                if (result.done()) break;
                helper.assertTrue(result.cell() > cursor, "Clearance cursor did not advance");
                var saved = new CompoundTag(); saved.putLong("Cell", result.cell());
                cursor = saved.getLong("Cell");
            }
        }
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            for (int y = minY; y <= maxY; y++)
                helper.assertTrue(level.getBlockState(new BlockPos(x, y, z)).isAir(), "Clearance left a clipped cell");
        helper.assertTrue(level.getBlockState(outside).is(Blocks.DIAMOND_BLOCK), "Clearing crossed horizontal boundary");
        helper.assertTrue(level.getBlockState(below).is(Blocks.DIAMOND_BLOCK), "Clearing crossed lower boundary");
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_clearance", template = "empty", timeoutTicks = 400)
    public static void caveTownClearsItsEntireSite(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(LuoxiaInnerDimension.LEVEL);
        var gap = new BlockPos(390 + 170, 64 + 30, 210 + 125);
        var room = new BlockPos(390 - 96, 64 + 4, 210 - 72);
        level.setBlock(gap, Blocks.CHEST.defaultBlockState(), 2);
        ((ChestBlockEntity) level.getBlockEntity(gap)).setItem(0, new ItemStack(Items.DIAMOND, 64));
        level.setBlock(room, Blocks.STONE.defaultBlockState(), 2);
        LuoxiaInnerRealmGenerator.buildCity(level, LuoxiaInnerRealmData.get(level), new java.util.Random(42));
        helper.assertTrue(level.getBlockState(gap).isAir() && level.getBlockEntity(gap) == null,
                "Cave town site retained an old chest");
        helper.assertTrue(level.getBlockState(room).isAir(), "Cave town left an interior obstruction");
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_clearance", template = "empty", timeoutTicks = 400)
    public static void exteriorSurveyAndTerrainClearSkyBlocks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().overworld();
        var site = LuoxiaSiteData.get(level);
        var saved = site.save(new CompoundTag());
        var origin = new BlockPos(-8000, 40, 8000);
        var gap = origin.offset(LuoxiaBlueprint.MIN_X, 220, LuoxiaBlueprint.MIN_Z);
        level.setBlock(gap, Blocks.CHEST.defaultBlockState(), 2);
        ((ChestBlockEntity) level.getBlockEntity(gap)).setItem(0, new ItemStack(Items.DIAMOND, 64));
        site.origin = origin;
        site.version = LuoxiaBlueprint.VERSION;
        site.phase = LuoxiaSiteData.Phase.SURVEY;
        site.paused = false;
        site.forceClearing = false;
        site.chunkIndex = site.operationIndex = 0;
        site.cellIndex = site.changedBlocks = 0;
        LuoxiaConstruction.tick(level);
        helper.assertTrue(site.paused, "Protected survey failed to report a site chest");
        site.paused = false; site.problem = ""; site.forceClearing = true;
        site.phase = LuoxiaSiteData.Phase.TERRAIN;
        site.chunkIndex = 0;
        site.cellIndex = 220 - LuoxiaBlueprint.MIN_Y;
        LuoxiaConstruction.tick(level);
        helper.assertTrue(!site.paused, "Terrain paused on an unrelated sky block");
        helper.assertTrue(level.getBlockState(gap).isAir() && level.getBlockEntity(gap) == null,
                "Terrain left an old sky chest inside the site");
        LuoxiaConstruction.release(level);
        level.getDataStorage().set("xiuxian_luoxia", LuoxiaSiteData.load(saved));
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_clearance", template = "empty", timeoutTicks = 1000000)
    public static void jindanClearsGapsAndHighBlocksAfterLegacyReload(GameTestHelper helper) {
        var level = helper.getLevel().getServer().overworld();
        JindanResidenceConstruction.cancel(level);
        var origin = new BlockPos(-4096, 40, 4096);
        var gap = origin.offset(176, 100, 162);
        var room = origin.offset(104, 15, 9);
        var above = origin.offset(0, 180, 0);
        var top = new BlockPos(origin.getX() - 178, level.getMaxBuildHeight() - 1, origin.getZ() - 164);
        var outside = top.west();
        level.setBlock(top, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(outside, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        level.setBlock(gap, Blocks.CHEST.defaultBlockState(), 2);
        ((ChestBlockEntity) level.getBlockEntity(gap)).setItem(0, new ItemStack(Items.DIAMOND, 64));
        level.setBlock(room, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(above, Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(JindanResidenceGenerator.generate(level, origin), "Residence failed to queue");
        var data = JindanResidenceConstruction.data(level);
        data.version = 4;
        data.phase = JindanResidenceConstruction.Phase.CLEAR_VERIFY;
        data.chunk = 4;
        data.cell = 17;
        var restored = JindanResidenceConstruction.Data.load(data.save(new CompoundTag()));
        JindanResidenceConstruction.release(level);
        level.getDataStorage().set("xiuxian_jindan_residences", restored);
        helper.succeedWhen(() -> {
            helper.assertTrue(restored.problem.isEmpty(), "Construction paused: " + restored.problem);
            helper.assertTrue(restored.origin == null && restored.contains(origin), "Construction incomplete");
            helper.assertTrue(level.getBlockState(gap).isAir() && level.getBlockEntity(gap) == null,
                    "Chest inside the site was not removed");
            helper.assertTrue(level.getBlockState(above).isAir(), "Block above the palace was not cleared");
            helper.assertTrue(level.getBlockState(top).isAir(), "World-height corner was missed");
            helper.assertTrue(level.getBlockState(outside).is(Blocks.DIAMOND_BLOCK), "Clearance exceeded site");
            helper.assertTrue(level.getBlockState(room).isAir(), "Room obstruction survived");
            helper.assertTrue(!level.getBlockState(origin.offset(30, 20, -103)).isAir(), "Palace wall missing");
        });
    }
}
