package xiuxian.sect;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("xiuxian_jindan_inner")
@PrefixGameTestTemplate(false)
public final class LuoxiaJindanGameTests {
    @GameTest(templateNamespace = "xiuxian_jindan_inner", template = "empty", timeoutTicks = 2000000,
            batch = "jindan_inner")
    public static void acceptedPalaceMigratesOnceResumesAndConnects(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var level = server.getLevel(LuoxiaInnerDimension.LEVEL);
        var realm = LuoxiaInnerRealmData.get(level);
        JindanResidenceConstruction.cancel(level);
        var data = JindanResidenceConstruction.data(level);
        data.origins.clear();
        var origin = LuoxiaJindanResidence.ORIGIN;
        realm.marker("jindan_residence", origin.offset(0, 3, 0));
        realm.marker("player_custom_jindan_marker", new BlockPos(380, 66, 210));
        var legacyRoad = new BlockPos(-72, 80, 172);
        var legacyLamp = new BlockPos(-40, 80, 166);
        var legacyStartLamp = new BlockPos(-4, 76, 160);
        var oldPalace = origin.offset(34, 20, 0);
        var top = new BlockPos(origin.getX() - 178, level.getMaxBuildHeight() - 1, origin.getZ() - 164);
        var preserved = List.of(top.west(), new BlockPos(390, 120, 210), new BlockPos(30, 80, 105),
                new BlockPos(0, -38, 300), new BlockPos(0, 48, 160), new BlockPos(-69, 84, 172),
                new BlockPos(221, 280, -600));
        for (var point : preserved) level.setBlock(point, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        for (var point : List.of(legacyRoad, legacyLamp, legacyStartLamp, oldPalace, top))
            level.setBlock(point, Blocks.STONE.defaultBlockState(), 2);
        long seed = realm.seed;
        var overworld = JindanResidenceConstruction.data(server.overworld()).save(new CompoundTag());
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        helper.assertTrue(origin.equals(data.origin) && data.phase == JindanResidenceConstruction.Phase.CLEAR,
                "Transplant did not start full clearance at the unique origin");
        Map<String, BlockPos> markers = Map.copyOf(realm.markers);
        var player = new RecordingVisitor(level);
        player.setGameMode(GameType.CREATIVE);
        var source = player.createCommandSourceStack().withPermission(2);
        helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian jindan inner status", source) == 1,
                "Inner status command failed");
        helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian jindan inner visit entrance", source) == 0,
                "Visit allowed before construction completed");
        helper.runAtTickTime(200, () -> {
            var current = JindanResidenceConstruction.data(level);
            var saved = current.save(new CompoundTag());
            var restored = JindanResidenceConstruction.Data.load(saved);
            helper.assertTrue(restored.save(new CompoundTag()).equals(saved), "Inner construction cursor lost on reload");
            JindanResidenceConstruction.release(level);
            level.getDataStorage().set("xiuxian_jindan_residences", restored);
            LuoxiaInnerRealmGenerator.ensureGenerated(level);
        });
        helper.succeedWhen(() -> {
            var current = JindanResidenceConstruction.data(level);
            helper.assertTrue(current.problem.isEmpty(), "Transplant paused: " + current.problem);
            helper.assertTrue(LuoxiaJindanResidence.isReady(level), "Transplant incomplete");
            helper.assertTrue(current.origin == null && current.origins.size() == 1, "Duplicate unique residence");
            helper.assertTrue(realm.version == 3 && realm.seed == seed && realm.markers.equals(markers),
                    "Transplant reset other realm data");
            helper.assertTrue(JindanResidenceConstruction.data(server.overworld()).save(new CompoundTag()).equals(overworld),
                    "Inner transplant changed Overworld construction data");
            for (var point : preserved) helper.assertTrue(level.getBlockState(point).is(Blocks.DIAMOND_BLOCK),
                    "Transplant erased another region at " + point);
            for (var point : List.of(legacyRoad, legacyLamp, legacyStartLamp, oldPalace, top))
                helper.assertTrue(level.getBlockState(point).isAir(), "Legacy/site cleanup missed " + point);
            var accepted = JindanResidenceGenerator.createPlan();
            for (int[] sample : new int[][]{{30,20,-103},{104,15,9},{28,32,0},{58,10,0},{-74,10,-76},
                    {0,30,-10},{0,17,0},{0,18,0},{-178,-2,-164}}) {
                BlockPos relative = new BlockPos(sample[0], sample[1], sample[2]);
                var expected = Blocks.AIR.defaultBlockState();
                for (var op : accepted.placements)
                    if (relative.getX() >= op.minX() && relative.getX() <= op.maxX()
                            && relative.getY() >= op.minY() && relative.getY() <= op.maxY()
                            && relative.getZ() >= op.minZ() && relative.getZ() <= op.maxZ()) expected = op.state();
                helper.assertTrue(level.getBlockState(origin.offset(relative)).equals(expected),
                        "Accepted blueprint differs after transplant at " + relative);
            }
            for(var floor:LuoxiaJindanResidence.approachPath())assertWalkway(helper,floor);
            for (int x = 178; x >= 122; x--)
                assertWalkway(helper, origin.offset(x, 7 + Math.min(2, (178 - x) / 20), 0));
            // The decorative arch occupies the centre block; its side passage remains open.
            for (int x = 121; x >= 92; x--) assertWalkway(helper, origin.offset(x, 9, 2));
            try {
                var landings = Map.of("entrance", origin.offset(177, 8, 0),
                        "palace", origin.offset(92, 10, 0), "furnace", origin.offset(4, 18, 4),
                        "view", origin.offset(205, 90, 205));
                for (String point : new String[]{"entrance", "palace", "furnace", "view"}) {
                    helper.assertTrue(server.getCommands().getDispatcher().execute("xiuxian jindan inner visit " + point, source) == 1,
                            "Completed palace visit failed: " + point);
                    helper.assertTrue(player.destination == level && player.target.equals(landings.get(point)),
                            "Incorrect visit target: " + point);
                    if (!point.equals("view")) {
                        var feet = player.target;
                        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()
                                || level.getBlockState(feet.below()).isAir()) {
                            helper.fail("Unsafe visit landing: " + feet);
                            return;
                        }
                    }
                }
                helper.assertTrue(player.getAbilities().flying, "Overview visit did not enable flight");
            } catch (CommandSyntaxException failure) { helper.fail("Visit command failed: " + failure.getMessage()); }
            var edit = origin.offset(176, 40, 162);
            level.setBlock(edit, Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
            LuoxiaInnerRealmGenerator.ensureGenerated(level);
            LuoxiaInnerRealmGenerator.ensureGenerated(level);
            helper.assertTrue(current.origin == null && current.origins.size() == 1
                            && level.getBlockState(edit).is(Blocks.EMERALD_BLOCK), "Re-entry rebuilt a completed residence");
            System.out.println("Jin-Dan INNER PASS: accepted palace, legacy migration, resume, unique site, retained landmarks, safe visits, walkable approach");
        });
    }

    private static void assertWalkway(GameTestHelper helper, BlockPos floor) {
        var level = helper.getLevel().getServer().getLevel(LuoxiaInnerDimension.LEVEL);
        helper.assertTrue(!level.getBlockState(floor).isAir(), "Walkway floor missing: " + floor);
        int step = level.getBlockState(floor.above()).getBlock() instanceof StairBlock ? 1 : 0;
        helper.assertTrue(level.getBlockState(floor.above(step + 1)).isAir()
                        && level.getBlockState(floor.above(step + 2)).isAir(), "Walkway lacks headroom: " + floor);
    }

    private static final class RecordingVisitor extends FakePlayer {
        ServerLevel destination;
        BlockPos target;

        RecordingVisitor(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "JindanVisitor"));
        }

        // FakePlayer's connection discards teleports, so capture the real command's requested destination.
        @Override public void teleportTo(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
            destination = level;
            target = BlockPos.containing(x, y, z);
        }
    }
}
