package xiuxian.sect;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import xiuxian.block.OrientalDecorationBlock;
import xiuxian.block.SectBlocks;
import xiuxian.block.SectInstrumentBlock;
import xiuxian.block.SectInstrumentBlockEntity;
import xiuxian.block.SectLampBlock;
import xiuxian.block.SectSeatBlock;
import xiuxian.cultivation.CultivationEvents;
import xiuxian.cultivation.CultivationPath;
import xiuxian.cultivation.FamilyOrigin;
import xiuxian.entity.SectSeatEntity;

@GameTestHolder("xiuxian_sect_assets")
@PrefixGameTestTemplate(false)
public final class SectResourceGameTests {
    @GameTest(templateNamespace = "xiuxian_sect_assets", template = "empty", timeoutTicks = 100)
    public static void allResourcesHaveItemsAndModelMatchingRotatedCollision(GameTestHelper helper) throws Exception {
        int seats = 0, lamps = 0, music = 0;
        helper.assertTrue(SectBlocks.resources().size() == 144, "Expected 144 registered sect resources");
        Path assets = Path.of(System.getProperty("xiuxian.verificationRoot"),
                "src/main/resources/assets/xiuxian/models/block");
        for (var entry : SectBlocks.resources().entrySet()) {
            var block = entry.getValue().get();
            helper.assertTrue(block.asItem() != net.minecraft.world.item.Items.AIR, "Missing BlockItem: " + entry.getKey());
            if (block instanceof SectSeatBlock) seats++;
            if (block instanceof SectLampBlock) lamps++;
            if (block instanceof SectInstrumentBlock) music++;
            if (!(block instanceof OrientalDecorationBlock)) continue;
            var elements = JsonParser.parseString(Files.readString(assets.resolve(entry.getKey() + ".json")))
                    .getAsJsonObject().getAsJsonArray("elements");
            int turn = 0;
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                var expected = Shapes.empty();
                for (var element : elements) {
                    var from = element.getAsJsonObject().getAsJsonArray("from");
                    var to = element.getAsJsonObject().getAsJsonArray("to");
                    double x1 = from.get(0).getAsDouble(), x2 = to.get(0).getAsDouble();
                    double z1 = from.get(2).getAsDouble(), z2 = to.get(2).getAsDouble();
                    for (int i = 0; i < turn; i++) {
                        double oldX1 = x1, oldX2 = x2;
                        x1 = 16 - z2; x2 = 16 - z1; z1 = oldX1; z2 = oldX2;
                    }
                    expected = Shapes.or(expected, Block.box(x1, from.get(1).getAsDouble(), z1,
                            x2, to.get(1).getAsDouble(), z2));
                }
                var state = block.defaultBlockState().setValue(OrientalDecorationBlock.FACING, direction);
                helper.assertTrue(!Shapes.joinIsNotEmpty(expected, state.getCollisionShape(helper.getLevel(), BlockPos.ZERO),
                        BooleanOp.NOT_SAME), "Collision/model mismatch: " + entry.getKey() + " " + direction);
                turn++;
            }
        }
        helper.assertTrue(seats == 8 && lamps == 20 && music == 12, "Interactive registrations incomplete");
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_sect_assets", template = "empty", timeoutTicks = 40)
    public static void lampsToggleLightAndRetainDirection(GameTestHelper helper) {
        var pos = site(helper);
        var player = player(helper);
        for (var resource : SectBlocks.resources().values()) {
            if (!(resource.get() instanceof SectLampBlock lamp)) continue;
            var state = lamp.defaultBlockState().setValue(SectLampBlock.FACING, Direction.EAST);
            helper.getLevel().setBlock(pos, state, 3);
            helper.assertTrue(state.getLightEmission() == 15, "Lamp initially dark");
            use(helper, pos, player);
            var unlit = helper.getLevel().getBlockState(pos);
            helper.assertTrue(!unlit.getValue(SectLampBlock.LIT) && unlit.getLightEmission() == 0
                    && unlit.getValue(SectLampBlock.FACING) == Direction.EAST, "Lamp failed to switch off");
            use(helper, pos, player);
            helper.assertTrue(helper.getLevel().getBlockState(pos).equals(state), "Lamp failed to switch on");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_sect_assets", template = "empty", timeoutTicks = 220)
    public static void instrumentsPlayScheduledPhraseAndStop(GameTestHelper helper) {
        var pos = site(helper);
        var player = player(helper);
        helper.getLevel().setBlock(pos, SectBlocks.state("sect_qin_table"), 3);
        var instrument = (SectInstrumentBlockEntity) helper.getLevel().getBlockEntity(pos);
        use(helper, pos, player);
        helper.runAtTickTime(5, () -> helper.assertTrue(instrument.notesPlayed() == 1 && instrument.note() == 1,
                "Instrument did not emit the first scheduled note"));
        helper.runAtTickTime(145, () -> {
            helper.assertTrue(instrument.notesPlayed() == 15 && instrument.note() == 0
                    && !helper.getLevel().getBlockState(pos).getValue(SectInstrumentBlock.PLAYING),
                    "Original 16-step phrase failed to finish");
            use(helper, pos, player);
            use(helper, pos, player);
        });
        helper.runAtTickTime(160, () -> {
            helper.assertTrue(instrument.notesPlayed() == 15, "Stopped instrument continued emitting notes");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "xiuxian_sect_assets", template = "empty", timeoutTicks = 40)
    public static void allInstrumentsSwitchTunesAndRestorePlaybackCursor(GameTestHelper helper) {
        var pos = site(helper);
        var player = player(helper);
        for (var resource : SectBlocks.resources().values()) {
            if (!(resource.get() instanceof SectInstrumentBlock)) continue;
            helper.getLevel().setBlock(pos, resource.get().defaultBlockState(), 3);
            var instrument = (SectInstrumentBlockEntity) helper.getLevel().getBlockEntity(pos);
            player.setShiftKeyDown(true);
            for (int i = 1; i <= 3; i++) {
                use(helper, pos, player);
                helper.assertTrue(instrument.tune() == i % 3, "Tune cycle failed");
            }
            player.setShiftKeyDown(false);
            use(helper, pos, player);
            instrument.playNextNote();
            instrument.playNextNote();
            var saved = instrument.saveWithoutMetadata();
            var restored = new SectInstrumentBlockEntity(pos, helper.getLevel().getBlockState(pos));
            restored.load(saved);
            helper.assertTrue(restored.note() == 2 && restored.tune() == 0, "Playback cursor lost on save/load");
            CompoundTag corrupt = new CompoundTag();
            corrupt.putInt("Tune", -100); corrupt.putInt("Note", Integer.MAX_VALUE);
            restored.load(corrupt);
            helper.assertTrue(restored.tune() >= 0 && restored.tune() < 3 && restored.note() >= 0 && restored.note() < 16,
                    "Malformed cursor was not sanitized");
            helper.getLevel().removeBlock(pos, false);
            helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null, "Destroyed instrument retained playback entity");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_sect_assets", template = "empty", timeoutTicks = 40)
    public static void savedInstrumentResumesFromItsCursor(GameTestHelper helper) {
        var pos = site(helper);
        var player = player(helper);
        helper.getLevel().setBlock(pos, SectBlocks.state("sect_jade_xiao"), 3);
        var original = (SectInstrumentBlockEntity) helper.getLevel().getBlockEntity(pos);
        original.nextTune();
        use(helper, pos, player);
        original.playNextNote();
        original.playNextNote();
        var saved = original.saveWithoutMetadata();
        var state = helper.getLevel().getBlockState(pos);
        helper.getLevel().removeBlock(pos, false);
        helper.getLevel().setBlock(pos, state, 3);
        var restored = new SectInstrumentBlockEntity(pos, state);
        restored.load(saved);
        helper.getLevel().setBlockEntity(restored);
        restored.onLoad();
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(restored.tune() == 1 && restored.note() == 3 && restored.notesPlayed() == 1,
                    "Reload restarted or duplicated playback instead of resuming at saved note 2");
            use(helper, pos, player);
        });
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(restored.notesPlayed() == 1, "Resumed instrument ignored stop");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "xiuxian_sect_assets", template = "empty", timeoutTicks = 40)
    public static void seatsEnforceOccupancyAndCleanOnDismountAndRemoval(GameTestHelper helper) {
        var pos = site(helper);
        var player = player(helper);
        var other = player(helper);
        float health = player.getHealth(), maxHealth = player.getMaxHealth();
        for (var resource : SectBlocks.resources().values()) {
            if (!(resource.get() instanceof SectSeatBlock chair)) continue;
            helper.getLevel().setBlock(pos, chair.defaultBlockState(), 3);
            player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            other.setPos(player.position());
            use(helper, pos, player);
            helper.assertTrue(player.getVehicle() instanceof SectSeatEntity, "Seat failed to mount");
            var seat = (SectSeatEntity) player.getVehicle();
            seat.positionRider(player);
            helper.assertTrue(Math.abs(player.getY() - (pos.getY() + chair.seatHeight())) < 0.001,
                    "Wrong sitting height");
            use(helper, pos, other);
            helper.assertTrue(!other.isPassenger() && seat.getPassengers().size() == 1 && seats(helper, pos) == 1,
                    "Occupied seat duplicated mount or accepted second passenger");
            helper.assertTrue(!seat.shouldBeSaved(), "Transient mount would persist across reload");
            helper.assertTrue(!seat.save(new CompoundTag()), "Player RootVehicle could serialize transient mount");
            var safeLanding = seat.getDismountLocationForPassenger(player);
            player.stopRiding();
            seat.tick();
            helper.assertTrue(seat.isRemoved() && !player.isPassenger(), "Empty mount was not cleaned");
            helper.assertTrue(player.position().distanceToSqr(safeLanding) < 0.001, "Unsafe dismount target");
            player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            use(helper, pos, player);
            seat = (SectSeatEntity) player.getVehicle();
            helper.getLevel().removeBlock(pos, false);
            helper.assertTrue(!player.isPassenger() && seat.isRemoved(), "Broken seat left a rider/mount");
        }
        helper.assertTrue(player.getHealth() == health && player.getMaxHealth() == maxHealth,
                "Seating altered cultivation health");
        helper.succeed();
    }

    @GameTest(templateNamespace = "xiuxian_sect_assets", template = "empty", timeoutTicks = 40)
    public static void seatsRejectObstructionDistanceAndCleanDeadRider(GameTestHelper helper) {
        var pos = site(helper);
        var player = player(helper);
        helper.getLevel().setBlock(pos, SectBlocks.state("sect_master_chair"), 3);
        helper.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        use(helper, pos, player);
        helper.assertTrue(!player.isPassenger() && seats(helper, pos) == 0, "Seat allowed obstructed headroom");
        helper.getLevel().removeBlock(pos.above(), false);
        player.setPos(pos.getX() + 8, pos.getY(), pos.getZ());
        use(helper, pos, player);
        helper.assertTrue(!player.isPassenger(), "Seat allowed remote mounting");
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.setShiftKeyDown(true);
        use(helper, pos, player);
        helper.assertTrue(!player.isPassenger(), "Sneaking use mounted player");
        player.setShiftKeyDown(false);
        use(helper, pos, player);
        var seat = (SectSeatEntity) player.getVehicle();
        player.setHealth(0);
        seat.tick();
        helper.assertTrue(seat.isRemoved() && !player.isPassenger(), "Dead rider left occupied mount");
        helper.succeed();
    }

    private static BlockPos site(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 1, 4));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            helper.getLevel().setBlock(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().removeBlock(pos.above(), false);
        helper.getLevel().removeBlock(pos.above(2), false);
        return pos;
    }

    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sect_test"));
        player.setGameMode(GameType.CREATIVE);
        CultivationEvents.selectIdentity(player, FamilyOrigin.MORTAL.id(), CultivationPath.WANDERER.id());
        var pos = helper.absolutePos(new BlockPos(4, 1, 4));
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        return player;
    }

    private static void use(GameTestHelper helper, BlockPos pos, FakePlayer player) {
        BlockState state = helper.getLevel().getBlockState(pos);
        state.getBlock().use(state, helper.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    private static int seats(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(SectSeatEntity.class, new AABB(pos).inflate(0.25)).size();
    }
}
