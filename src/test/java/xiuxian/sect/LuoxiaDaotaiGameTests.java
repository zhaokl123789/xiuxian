package xiuxian.sect;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_daotai_inner")
@PrefixGameTestTemplate(false)
public final class LuoxiaDaotaiGameTests {
    private LuoxiaDaotaiGameTests() {}

    @GameTest(templateNamespace = "xiuxian_daotai_inner", template = "empty", timeoutTicks = 2000000,batch="daotai_inner")
    public static void acceptedPalaceMigratesOnceAndResumes(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(LuoxiaInnerDimension.LEVEL);
        helper.assertTrue(level != null, "Inner dimension missing");
        var realm = LuoxiaInnerRealmData.get(level);
        var data = DaotaiResidenceConstruction.data(level);
        helper.assertTrue(realm.generated && realm.version == 3, "Expected current realm generation");
        // Reproduce an existing v3 realm with the obsolete elevated landmark.
        DaotaiResidenceConstruction.cancel(level);
        data.origins.clear();
        realm.marker("daotai_residence", new BlockPos(0, 254, 430));
        realm.marker("player_custom_marker", new BlockPos(380, 66, 210));
        var oldPalace = new BlockPos(0, 250, 430);
        var oldStair = new BlockPos(0, 104, 211);
        var oldLamp = new BlockPos(6, 107, 211);
        var outsideCleanup = new BlockPos(71, 250, 430);
        var preserved = List.of(new BlockPos(390, 120, 210), new BlockPos(30, 80, 105),
                new BlockPos(0, -38, 300), new BlockPos(0, 48, 160), outsideCleanup);
        for (var point : preserved) level.setBlock(point, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        for (var point : List.of(oldPalace, oldStair, oldLamp)) level.setBlock(point, Blocks.STONE.defaultBlockState(), 2);
        var edge = LuoxiaDaotaiResidence.ORIGIN.offset(220, 210, 210);
        var outside = edge.east();
        level.setBlock(edge, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(outside, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        long seed = realm.seed;
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        Map<String, BlockPos> markers = Map.copyOf(realm.markers);
        helper.assertTrue(data.origin.equals(LuoxiaDaotaiResidence.ORIGIN), "Unique transplant not queued");
        helper.runAtTickTime(100, () -> {
            var saved = data.save(new CompoundTag());
            var restored = DaotaiResidenceConstruction.Data.load(saved);
            helper.assertTrue(restored.save(new CompoundTag()).equals(saved), "Construction cursor lost on NBT reload");
            DaotaiResidenceConstruction.release(level);
            level.getDataStorage().set("xiuxian_daotai_residences", restored);
            LuoxiaInnerRealmGenerator.ensureGenerated(level);
        });
        helper.succeedWhen(() -> {
            var current = DaotaiResidenceConstruction.data(level);
            helper.assertTrue(current.problem.isEmpty(), "Inner transplant paused: " + current.problem);
            helper.assertTrue(LuoxiaDaotaiResidence.isReady(level), "Inner transplant incomplete");
            helper.assertTrue(current.origin == null && current.origins.size() == 1, "Duplicate unique residence");
            helper.assertTrue(realm.version == 3 && realm.seed == seed && realm.markers.equals(markers),
                    "Dao-Tai update reset other realm data");
            for (var point : preserved) helper.assertTrue(level.getBlockState(point).is(Blocks.DIAMOND_BLOCK),
                    "Transplant erased another region at " + point);
            for (var point : List.of(oldPalace, oldStair, oldLamp, edge)) helper.assertTrue(level.getBlockState(point).isAir(),
                    "Legacy/site clearing missed " + point);
            helper.assertTrue(level.getBlockState(outside).is(Blocks.DIAMOND_BLOCK), "Site clearing exceeded footprint");
            var origin = LuoxiaDaotaiResidence.ORIGIN;
            for (int[] p : new int[][]{{182,28,20},{134,48,20},{86,66,20},{80,74,-58},
                    {54,113,-92},{34,143,-92},{48,160,-150},{0,222,-150}}) {
                helper.assertTrue(!level.getBlockState(origin.offset(p[0],p[1],p[2])).isAir(),
                        "Approved landmark missing: " + java.util.Arrays.toString(p));
            }
            helper.assertTrue(origin.getY() + 222 < level.getMaxBuildHeight(), "Palace exceeds height limit");
            helper.assertTrue(level.getBlockState(origin.offset(-19,74,-97)).getLightEmission() == 15,
                    "Interior light missing after transplant");
            helper.assertTrue(level.getBlockState(origin.offset(-22,75,-107)).getBlock()
                    .builtInRegistryHolder().key().location().getPath().startsWith("daotai_furniture_"),
                    "Approved furnishing missing");
            var route=new java.util.ArrayList<>(LuoxiaDaotaiResidence.approachPath());
            for(int z=210;z>=-75;z--)route.add(origin.offset(0,DaotaiResidenceGenerator.axisHeight(z),z));
            for(var floor:route) {
                helper.assertTrue(!level.getBlockState(floor).isAir(), "Walkway floor missing: " + floor);
                int step = level.getBlockState(floor.above()).getBlock() instanceof StairBlock ? 1 : 0;
                helper.assertTrue(level.getBlockState(floor.above(step + 1)).isAir()
                                && level.getBlockState(floor.above(step + 2)).isAir(),
                        "Walkway lacks headroom: " + floor);
            }
            level.setBlock(origin.offset(44,76,-100), Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
            LuoxiaInnerRealmGenerator.ensureGenerated(level);
            LuoxiaInnerRealmGenerator.ensureGenerated(level);
            helper.assertTrue(current.origin == null && current.origins.size() == 1
                            && level.getBlockState(origin.offset(44,76,-100)).is(Blocks.EMERALD_BLOCK),
                    "A completed residence was rebuilt on re-entry");
            System.out.println("Dao-Tai INNER PASS: full build, v3 migration, resume, unique landmark, retained city/sect/vein/boss, walkable approach");
        });
    }
}
