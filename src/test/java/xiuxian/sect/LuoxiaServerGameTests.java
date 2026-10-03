package xiuxian.sect;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import xiuxian.cultivation.CultivationCapability;
import xiuxian.cultivation.CultivationAttributeEffects;
import xiuxian.cultivation.CultivationData;
import xiuxian.cultivation.CultivationPath;
import xiuxian.cultivation.CultivationRealm;
import xiuxian.cultivation.FamilyOrigin;
import xiuxian.item.XiuxianItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Runs the complete production construction through Forge's real server tick events. */
@GameTestHolder("xiuxian")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = "xiuxian")
public final class LuoxiaServerGameTests {
    private static final BlockPos ORIGIN = new BlockPos(4096, 64, 4096);
    private static final int TIMEOUT_TICKS = 180000;
    private static final int FLUID_TICKET_RADIUS = 4;
    private static final TicketType<ChunkPos> FLUID_OBSERVATION_TICKET = TicketType.create(
            "xiuxian_luoxia_fluid_test", Comparator.comparingLong(ChunkPos::toLong), 400);
    private static final Map<ServerLevel, FluidObservation> FLUID_OBSERVATIONS = new IdentityHashMap<>();
    private static final Map<ServerLevel, GameProfile> DECREE_TEST_OPERATORS = new IdentityHashMap<>();
    private static final int[][] WATERFALLS = {{-86, -105}, {91, -186}, {-138, -107}, {141, -183}};

    private LuoxiaServerGameTests() {}

    @GameTest(templateNamespace = "forge", template = "empty", timeoutTicks = TIMEOUT_TICKS, batch = "luoxia")
    public static void fullConstruction(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().overworld();
        try {
            runFullConstruction(helper, level);
        } catch (RuntimeException | Error failure) {
            closeFluidObservation(level);
            removeDecreeTestOperator(level);
            throw failure;
        }
    }

    private static void runFullConstruction(GameTestHelper helper, ServerLevel level) {
        LuoxiaSiteData initialSite = LuoxiaSiteData.get(level);
        helper.assertTrue(initialSite.origin == null, "Run this test in a fresh GameTest world; an existing sect is protected");
        FakePlayer observer = FakePlayerFactory.get(level, new GameProfile(
                UUID.fromString("9260a4b1-7c6d-4fc7-9f5a-756c790c18a9"), "LuoxiaVerification"));
        CultivationData cultivation = observer.getCapability(CultivationCapability.CULTIVATION)
                .orElseThrow(() -> new IllegalStateException("Cultivation capability was not attached in the real Forge runtime"));
        cultivation.begin(FamilyOrigin.CULTIVATOR, CultivationPath.SECT, RandomSource.create(12345));
        helper.assertTrue(cultivation.grantDirectRealm(CultivationRealm.PURPLE_MANSION, RandomSource.create(54321)),
                "Could not initialize a Purple Mansion cultivation identity");
        CultivationAttributeEffects.applyAndPreserveHealth(observer, cultivation);
        float initialMaxHealth = observer.getMaxHealth();
        float initialHealth = observer.getHealth();
        helper.assertTrue(initialMaxHealth > 20, "Purple Mansion health bonus was not applied in the actual server runtime");
        CompoundTag identity = cultivation.serializeNBT().copy();
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPermission(4).withSuppressedOutput();

        expectCommand(helper, source, "plan 4096 200 4096", 0);
        expectCommand(helper, source, "plan 4096 -64 4096", 0);
        helper.assertTrue(initialSite.origin == null, "Invalid height changed the site");
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether dimension is missing from the test server");
        expectCommand(helper, source.withLevel(nether), "plan 4096 64 4096", 0);
        helper.assertTrue(initialSite.origin == null, "A Nether command planned an overworld building");
        expectCommand(helper, source, "build", 0);
        verifyRejectedWrite(helper, level, initialSite);
        observer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(XiuxianItems.LUOXIA_CONSTRUCTION_DECREE.get()));
        verifyDecreeRejections(helper, level, nether, observer, initialSite);

        BlockPos protectedChest = ORIGIN.offset(LuoxiaBlueprint.MIN_X + 1, 0, LuoxiaBlueprint.MIN_Z + 1);
        level.setBlock(protectedChest, Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(protectedChest);
        helper.assertTrue(chest != null, "Test chest block entity was not created");
        chest.setItem(0, new ItemStack(Items.DIAMOND));
        BlockPos sentinel = ORIGIN.offset(LuoxiaBlueprint.MIN_X - 1, 64, LuoxiaBlueprint.MIN_Z);
        level.setBlock(sentinel, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        BlockPos decreeGround = ORIGIN.offset(0, 0, 120);
        useDecreeOn(helper, observer, decreeGround, Direction.UP, false, true);
        helper.assertTrue(ORIGIN.equals(initialSite.origin) && initialSite.phase == LuoxiaSiteData.Phase.SURVEY
                        && initialSite.active() && initialSite.changedBlocks == 0,
                "The decree did not start survey at the origin 120 blocks north of the clicked ground");
        CompoundTag startedSite = initialSite.save(new CompoundTag()).copy();
        useDecreeInAir(helper, observer, false, true);
        useDecreeOn(helper, observer, decreeGround.offset(32, 0, 32), Direction.UP, false, true);
        ItemStack decree = observer.getMainHandItem();
        observer.getCooldowns().removeCooldown(decree.getItem());
        InteractionResult chestFirstUse = decree.onItemUseFirst(new UseOnContext(observer, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(protectedChest).relative(Direction.UP, 0.5),
                        Direction.UP, protectedChest, false)));
        verifyDecreeResult(helper, observer, decree, 1, chestFirstUse, true);
        helper.assertTrue(initialSite.save(new CompoundTag()).equals(startedSite),
                "A repeated ordinary decree click changed or restarted the construction site");
        useDecreeInAir(helper, observer, true, true);
        helper.assertTrue(initialSite.paused && !initialSite.active(), "Sneaking with the decree did not pause the job");
        verifyDecreeCooldown(helper, observer, initialSite);
        useDecreeInAir(helper, observer, false, true);
        helper.assertTrue(initialSite.paused, "An ordinary decree click resumed a paused job");
        useDecreeInAir(helper, observer, true, true);
        helper.assertTrue(!initialSite.paused && initialSite.active(), "Sneaking with the decree did not resume the job");
        identityUnchanged(helper, cultivation, identity);
        helper.assertTrue(observer.getMaxHealth() == initialMaxHealth && observer.getHealth() == initialHealth,
                "Decree interactions changed Purple Mansion health");
        System.out.println("Luoxia server test: real decree interactions started construction and safely queried/paused/resumed it");

        int[] stage = {0};
        long[] pauseTick = {-1};
        long[] pausedCell = {0};
        int[] pausedChunk = {0};
        int[] pausedOperation = {0};
        long started = System.nanoTime();
        LuoxiaSiteData.Phase[] previous = {initialSite.phase};
        helper.runAtTickTime(TIMEOUT_TICKS - 1L, () -> {
            closeFluidObservation(level);
            removeDecreeTestOperator(level);
        });
        helper.onEachTick(() -> {
            try {
                LuoxiaSiteData site = LuoxiaSiteData.get(level);
                if (previous[0] != site.phase || helper.getTick() % 200 == 0) {
                    System.out.printf("Luoxia server test: tick=%d phase=%s chunk=%d/%d changed=%d%n",
                            helper.getTick(), site.phase, site.chunkIndex, LuoxiaConstruction.chunkCount(ORIGIN), site.changedBlocks);
                    previous[0] = site.phase;
                }
                if (stage[0] == 0) {
                    if (!site.paused) return;
                    helper.assertTrue(site.phase == LuoxiaSiteData.Phase.SURVEY && site.changedBlocks == 0,
                            "A protected chest was encountered after destructive writes");
                    helper.assertTrue(level.getBlockEntity(protectedChest) == chest && chest.getItem(0).is(Items.DIAMOND),
                            "Survey damaged the protected chest or its inventory");
                    helper.assertTrue(!site.problem.isEmpty(), "Survey pause omitted the protection reason");
                    level.setBlock(protectedChest, Blocks.AIR.defaultBlockState(), 3);
                    useDecreeInAir(helper, observer, true, true);
                    stage[0] = 1;
                    System.out.println("Luoxia server test: protected chest preserved; survey resumed");
                } else if (stage[0] == 1) {
                    helper.assertTrue(!site.paused, "Construction unexpectedly paused: " + site.problem);
                    if (site.phase != LuoxiaSiteData.Phase.TERRAIN || site.changedBlocks == 0) return;
                    useDecreeInAir(helper, observer, true, true);
                    pausedChunk[0] = site.chunkIndex;
                    pausedOperation[0] = site.operationIndex;
                    pausedCell[0] = site.cellIndex;
                    pauseTick[0] = helper.getTick();
                    LuoxiaSiteData restored = LuoxiaSiteData.load(site.save(new CompoundTag()));
                    helper.assertTrue(restored.paused && restored.phase == site.phase && ORIGIN.equals(restored.origin)
                                    && restored.cellIndex == pausedCell[0] && restored.chunkIndex == pausedChunk[0]
                                    && restored.changedBlocks == site.changedBlocks && restored.validCursor(),
                            "Saved live construction cursor did not survive NBT reload");
                    LuoxiaConstruction.release(level);
                    CompoundTag invalidCursor = site.save(new CompoundTag());
                    invalidCursor.putLong("Cell", Long.MAX_VALUE);
                    LuoxiaSiteData invalid = LuoxiaSiteData.load(invalidCursor);
                    level.getDataStorage().set("xiuxian_luoxia", invalid);
                    useDecreeInAir(helper, observer, true, false);
                    expectCommand(helper, source, "resume", 0);
                    helper.assertTrue(invalid.paused && invalid.cellIndex == Long.MAX_VALUE,
                            "Resume accepted an out-of-range saved cursor");
                    level.getDataStorage().set("xiuxian_luoxia", restored);
                    restored.setDirty();
                    helper.assertTrue(LuoxiaSiteData.get(level) == restored && restored != site,
                            "The construction worker is still using the pre-reload site object");
                    stage[0] = 2;
                } else if (stage[0] == 2) {
                    helper.assertTrue(site.paused && site.cellIndex == pausedCell[0] && site.chunkIndex == pausedChunk[0]
                                    && site.operationIndex == pausedOperation[0], "A paused construction cursor advanced");
                    if (helper.getTick() < pauseTick[0] + 10) return;
                    useDecreeInAir(helper, observer, true, true);
                    identityUnchanged(helper, cultivation, identity);
                    helper.assertTrue(observer.getMaxHealth() == initialMaxHealth && observer.getHealth() == initialHealth,
                            "Construction reset Purple Mansion health");
                    stage[0] = 3;
                    System.out.println("Luoxia server test: reloaded site object resumed after a stable pause; corrupt cursor rejected");
                } else if (stage[0] == 3) {
                    helper.assertTrue(!site.paused, "Construction unexpectedly paused: " + site.problem);
                    if (site.phase != LuoxiaSiteData.Phase.COMPLETE) return;
                    verifyLiveGeometry(helper, level);
                    helper.assertTrue(level.getBlockState(sentinel).is(Blocks.GOLD_BLOCK), "Construction wrote outside its boundary");
                    identityUnchanged(helper, cultivation, identity);
                    expectCommand(helper, source, "locate", 1);
                    expectCommand(helper, source, "status", 1);
                    expectCommand(helper, source, "build", 0);
                    expectCommand(helper, source, "plan 8192 64 8192", 0);
                    helper.assertTrue(ORIGIN.equals(site.origin) && site.phase == LuoxiaSiteData.Phase.COMPLETE,
                            "A completed sect was overwritten by a repeated command");
                    CompoundTag completedSite = site.save(new CompoundTag()).copy();
                    useDecreeInAir(helper, observer, false, true);
                    useDecreeOn(helper, observer, decreeGround, Direction.UP, false, true);
                    useDecreeInAir(helper, observer, true, true);
                    helper.assertTrue(site.save(new CompoundTag()).equals(completedSite),
                            "Completed-site decree queries or entrance visits changed the construction data");
                    identityUnchanged(helper, cultivation, identity);
                    helper.assertTrue(observer.getMaxHealth() == initialMaxHealth && observer.getHealth() == initialHealth,
                            "Completed-site decree interactions reset Purple Mansion health");
                    beginFluidObservation(level);
                    stage[0] = 4;
                    System.out.println("Luoxia server test: construction complete; loading fluid-ticking observation chunks");
                } else if (stage[0] == 4) {
                    FluidObservation observation = FLUID_OBSERVATIONS.get(level);
                    helper.assertTrue(observation != null, "Fluid observation tickets disappeared before the test completed");
                    observation.refreshTickets();
                    if (!observation.ready()) return;
                    if (observation.startedTick < 0) {
                        observation.startedTick = helper.getTick();
                        System.out.println("Luoxia server test: observation chunks are fluid-ticking; settling water for 200 ticks");
                    }
                    if (helper.getTick() < observation.startedTick + 200) return;
                    verifyLiveGeometry(helper, level);
                    verifyFluids(helper, level);
                    closeFluidObservation(level);
                    removeDecreeTestOperator(level);
                    System.out.printf("Luoxia REAL FORGE SERVER PASS: origin=%s changed=%d ticks=%d elapsed=%.2fs%n",
                            ORIGIN.toShortString(), site.changedBlocks, helper.getTick(),
                            (System.nanoTime() - started) / 1_000_000_000.0);
                    stage[0] = 5;
                    helper.succeed();
                }
            } catch (RuntimeException | Error failure) {
                closeFluidObservation(level);
                removeDecreeTestOperator(level);
                throw failure;
            }
        });
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        for (FluidObservation observation : new ArrayList<>(FLUID_OBSERVATIONS.values())) {
            if (observation.level.getServer() == event.getServer()) observation.close();
        }
        for (ServerLevel level : new ArrayList<>(DECREE_TEST_OPERATORS.keySet())) {
            if (level.getServer() == event.getServer()) removeDecreeTestOperator(level);
        }
    }

    private static void removeDecreeTestOperator(ServerLevel level) {
        GameProfile profile = DECREE_TEST_OPERATORS.remove(level);
        if (profile != null) level.getServer().getPlayerList().getOps().remove(profile);
    }

    private static void verifyDecreeRejections(GameTestHelper helper, ServerLevel level, ServerLevel nether,
                                              FakePlayer player, LuoxiaSiteData site) {
        BlockPos ground = ORIGIN.offset(0, 0, 120);
        level.setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
        CompoundTag emptySite = site.save(new CompoundTag()).copy();
        player.setGameMode(GameType.SURVIVAL);
        useDecreeOn(helper, player, ground, Direction.UP, false, false);
        helper.assertTrue(site.save(new CompoundTag()).equals(emptySite), "A survival decree interaction changed the site");

        player.setGameMode(GameType.CREATIVE);
        helper.assertTrue(player.isCreative() && !player.hasPermissions(2),
                "The unprivileged creative decree fixture already has operator permission");
        useDecreeOn(helper, player, ground, Direction.UP, false, false);
        helper.assertTrue(site.save(new CompoundTag()).equals(emptySite), "An unprivileged creative decree interaction changed the site");

        GameProfile profile = player.getGameProfile();
        DECREE_TEST_OPERATORS.put(level, profile);
        // GameTestServer grants ordinary operators level zero; this test fixture needs the production level-two permission.
        level.getServer().getPlayerList().getOps().add(new ServerOpListEntry(profile, 2, false));
        helper.assertTrue(player.hasPermissions(2), "The decree operator fixture could not obtain level-two permission");
        useDecreeInAir(helper, player, false, true);
        useDecreeInAir(helper, player, true, true);
        useDecreeOn(helper, player, ground, Direction.EAST, false, true);
        helper.assertTrue(site.save(new CompoundTag()).equals(emptySite), "Air or side-face decree queries started construction");

        for (int y : new int[]{200, -64}) {
            BlockPos invalidGround = new BlockPos(ground.getX(), y, ground.getZ());
            level.setBlock(invalidGround, Blocks.STONE.defaultBlockState(), 3);
            useDecreeOn(helper, player, invalidGround, Direction.UP, false, false);
            helper.assertTrue(site.save(new CompoundTag()).equals(emptySite), "A decree at invalid height changed the site");
        }
        player.setServerLevel(nether);
        try {
            nether.setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
            useDecreeOn(helper, player, ground, Direction.UP, false, false);
            useDecreeInAir(helper, player, false, false);
            helper.assertTrue(site.save(new CompoundTag()).equals(emptySite), "A Nether decree interaction changed the overworld site");
        } finally {
            player.setServerLevel(level);
        }
        System.out.println("Luoxia server test: decree survival, non-operator, invalid-height and Nether rejection PASS; air/side queries did not build");
    }

    private static void useDecreeOn(GameTestHelper helper, FakePlayer player, BlockPos ground,
                                    Direction face, boolean sneaking, boolean accepted) {
        ItemStack stack = player.getMainHandItem();
        int count = stack.getCount();
        player.getCooldowns().removeCooldown(stack.getItem());
        player.setShiftKeyDown(sneaking);
        try {
            UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).relative(face, 0.5), face, ground, false));
            InteractionResult result = stack.useOn(context);
            verifyDecreeResult(helper, player, stack, count, result, accepted);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    private static void useDecreeInAir(GameTestHelper helper, FakePlayer player, boolean sneaking, boolean accepted) {
        ItemStack stack = player.getMainHandItem();
        int count = stack.getCount();
        player.getCooldowns().removeCooldown(stack.getItem());
        player.setShiftKeyDown(sneaking);
        try {
            InteractionResultHolder<ItemStack> result = stack.use(player.level(), player, InteractionHand.MAIN_HAND);
            verifyDecreeResult(helper, player, stack, count, result.getResult(), accepted);
            helper.assertTrue(result.getObject() == stack, "Decree air use replaced the held item stack");
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    private static void verifyDecreeResult(GameTestHelper helper, FakePlayer player, ItemStack stack, int count,
                                          InteractionResult result, boolean accepted) {
        helper.assertTrue(result.consumesAction() == accepted,
                "Decree interaction returned " + result + " instead of " + (accepted ? "accepting" : "rejecting") + " the action");
        helper.assertTrue(player.getMainHandItem() == stack && stack.getCount() == count
                        && stack.is(XiuxianItems.LUOXIA_CONSTRUCTION_DECREE.get()),
                "Decree interaction consumed or replaced the held decree");
        helper.assertTrue(player.getCooldowns().isOnCooldown(stack.getItem()) == accepted,
                "Decree interaction did not apply cooldown only to successful use");
    }

    private static void verifyDecreeCooldown(GameTestHelper helper, FakePlayer player, LuoxiaSiteData site) {
        ItemStack stack = player.getMainHandItem();
        CompoundTag pausedSite = site.save(new CompoundTag()).copy();
        player.setShiftKeyDown(true);
        try {
            InteractionResultHolder<ItemStack> repeated = stack.use(player.level(), player, InteractionHand.MAIN_HAND);
            verifyDecreeResult(helper, player, stack, 1, repeated.getResult(), true);
            helper.assertTrue(site.save(new CompoundTag()).equals(pausedSite),
                    "A repeated sneaking click during cooldown toggled the job again");
        } finally {
            player.setShiftKeyDown(false);
        }
        for (int tick = 0; tick < 19; tick++) player.getCooldowns().tick();
        helper.assertTrue(player.getCooldowns().isOnCooldown(stack.getItem()), "The decree cooldown expired before twenty ticks");
        player.getCooldowns().tick();
        helper.assertTrue(!player.getCooldowns().isOnCooldown(stack.getItem()), "The decree cooldown lasted longer than twenty ticks");
        System.out.println("Luoxia server test: decree first-use handling and twenty-tick repeat suppression PASS");
    }

    private static void beginFluidObservation(ServerLevel level) {
        FluidObservation observation = new FluidObservation(level);
        FLUID_OBSERVATIONS.put(level, observation);
        observation.refreshTickets();
    }

    private static void closeFluidObservation(ServerLevel level) {
        FluidObservation observation = FLUID_OBSERVATIONS.get(level);
        if (observation != null) observation.close();
    }

    private static final class FluidObservation {
        private final ServerLevel level;
        private final Set<ChunkPos> tickets = new LinkedHashSet<>();
        private final Set<ChunkPos> tickingChunks = new LinkedHashSet<>();
        private long startedTick = -1;
        private boolean closed;

        FluidObservation(ServerLevel level) {
            this.level = level;
            for (int[] waterfall : WATERFALLS) addCenter(waterfall[0], waterfall[1]);
            addCenter(160, -200);
            addCenter(-160, -104);
            for (ChunkPos center : tickets) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dx = -3; dx <= 3; dx++) tickingChunks.add(new ChunkPos(center.x + dx, center.z + dz));
                }
            }
        }

        private void addCenter(int x, int z) {
            tickets.add(new ChunkPos(ORIGIN.offset(x, 0, z)));
        }

        void refreshTickets() {
            if (closed) return;
            for (ChunkPos chunk : tickets) {
                level.getChunkSource().addRegionTicket(FLUID_OBSERVATION_TICKET, chunk, FLUID_TICKET_RADIUS, chunk, true);
            }
        }

        boolean ready() {
            if (closed) return false;
            // Match ServerLevel's scheduled-fluid predicate; entity loading does not require entity ticking.
            for (ChunkPos chunk : tickingChunks) {
                if (!level.areEntitiesLoaded(chunk.toLong()) || !level.getChunkSource().isPositionTicking(chunk.toLong())) return false;
            }
            return true;
        }

        void close() {
            if (closed) return;
            closed = true;
            for (ChunkPos chunk : tickets) {
                level.getChunkSource().removeRegionTicket(FLUID_OBSERVATION_TICKET, chunk, FLUID_TICKET_RADIUS, chunk, true);
            }
            FLUID_OBSERVATIONS.remove(level);
        }
    }

    private static void verifyFluids(GameTestHelper helper, ServerLevel level) {
        for (int[] waterfall : WATERFALLS) {
            int x = waterfall[0], z = waterfall[1], top = LuoxiaTerrain.heightAt(x, z);
            BlockState source = local(level, x, top + 1, z);
            helper.assertTrue(source.getFluidState().is(FluidTags.WATER) && source.getFluidState().isSource(),
                    "A waterfall source drained after fluid simulation at " + x + "," + z);
            BlockState column = local(level, x, Math.max(1, top / 2), z + 1);
            helper.assertTrue(column.getFluidState().is(FluidTags.WATER)
                            && column.getFluidState().getValue(FlowingFluid.FALLING),
                    "A waterfall lost its downward-flowing water column at " + x + "," + z);
            BlockState basin = local(level, x, -1, z + 2);
            helper.assertTrue(basin.getFluidState().is(FluidTags.WATER) && basin.getFluidState().isSource(),
                    "A waterfall basin drained after fluid simulation at " + x + "," + z);
            helper.assertTrue(local(level, x, top + 1, z - 2).is(Blocks.STONE_BRICKS)
                            && local(level, x - 3, top + 1, z).is(Blocks.STONE_BRICKS)
                            && local(level, x + 3, top + 1, z).is(Blocks.STONE_BRICKS),
                    "A waterfall's source-pool walls are missing");
        }
        for (int[] shore : new int[][]{{160, -200}, {-160, -104}}) {
            int x = shore[0], z = shore[1], outside = x + Integer.signum(x);
            helper.assertTrue(local(level, x, -1, z).is(Blocks.GRAVEL), "The lake shoreline was displaced");
            for (int y = -1; y <= 0; y++) {
                helper.assertTrue(!local(level, outside, y, z).getFluidState().is(FluidTags.WATER),
                        "Lake water escaped the construction boundary at " + outside + "," + y + "," + z);
            }
        }
        System.out.println("Luoxia server test: active waterfall sources, falling columns, basins, shoreline and dry passages PASS");
    }

    private static void expectCommand(GameTestHelper helper, CommandSourceStack source, String command, int expected) {
        int result = source.getServer().getCommands().performPrefixedCommand(source, "xiuxian luoxia " + command);
        helper.assertTrue(result == expected, "Command returned " + result + " instead of " + expected + ": " + command);
    }

    private static void verifyRejectedWrite(GameTestHelper helper, ServerLevel level, LuoxiaSiteData initialSite) {
        LuoxiaSiteData rejected = new LuoxiaSiteData();
        rejected.origin = new BlockPos(ORIGIN.getX(), level.getMaxBuildHeight() + 16, ORIGIN.getZ());
        rejected.phase = LuoxiaSiteData.Phase.TERRAIN;
        level.getChunk((ORIGIN.getX() + LuoxiaBlueprint.MIN_X) >> 4, (ORIGIN.getZ() + LuoxiaBlueprint.MIN_Z) >> 4);
        level.getDataStorage().set("xiuxian_luoxia", rejected);
        try {
            LuoxiaConstruction.tick(level);
            helper.assertTrue(rejected.paused && rejected.changedBlocks == 0 && rejected.cellIndex == 0
                            && rejected.chunkIndex == 0 && !rejected.problem.isEmpty(),
                    "A rejected world write was counted or advanced the construction cursor");
        } finally {
            LuoxiaConstruction.release(level);
            level.getDataStorage().set("xiuxian_luoxia", initialSite);
        }
        System.out.println("Luoxia server test: rejected world write paused without advancing or counting a block");
    }

    private static void identityUnchanged(GameTestHelper helper, CultivationData data, CompoundTag before) {
        helper.assertTrue(data.isInitialized() && data.realm() == CultivationRealm.PURPLE_MANSION
                && data.serializeNBT().equals(before), "Sect construction changed the player's cultivation identity");
    }

    private static void verifyLiveGeometry(GameTestHelper helper, ServerLevel level) {
        for (int z = 112; z >= -162; z--) {
            int floor = LuoxiaTerrain.pathHeight(z);
            helper.assertTrue(!local(level, 0, floor, z).isAir(), "Ascent has no floor at z=" + z);
            helper.assertTrue(local(level, 0, floor + 2, z).isAir() && local(level, 0, floor + 3, z).isAir(),
                    "Ascent has obstructed headroom at z=" + z);
            BlockState step = local(level, 0, floor + 1, z);
            helper.assertTrue(step.isAir() || step.getBlock() instanceof StairBlock
                            && step.getValue(StairBlock.FACING) == Direction.NORTH,
                    "Ascent has an incorrectly oriented/blocked step at z=" + z);
        }
        for (int[] door : new int[][]{{0, 19, 64}, {0, 49, -13}, {0, 85, -91},
                {0, 134, -169}, {0, 134, -209}, {32, 134, -189}, {-32, 134, -189}}) {
            passage(helper, level, door[0], door[1], door[2]);
        }
        for (int z = -208; z <= -170; z++) passage(helper, level, 0, 134, z);
        for (int x = -112; x <= -63; x++) passage(helper, level, x, 85, -103);
        for (int x = 70; x <= 120; x++) passage(helper, level, x, 133, -180);
        LuoxiaBlueprint blueprint = LuoxiaBlueprint.create();
        int checkedRoof = 0;
        for (int y = 149; y <= 188; y++) {
            for (int z = -217; z <= -157; z += 3) {
                for (int x = -40; x <= 40; x += 4) {
                    BlockState expected = blueprint.sampleArchitecture(x, y, z);
                    if (expected != null && expected.is(Blocks.DEEPSLATE_TILES)) {
                        helper.assertTrue(local(level, x, y, z).is(Blocks.DEEPSLATE_TILES),
                                "Live summit roof differs from the blueprint");
                        checkedRoof++;
                    }
                }
            }
        }
        helper.assertTrue(checkedRoof > 100, "Summit tiled roof was not constructed");
        // These cross both positive and negative local chunk boundaries through the actual world.
        for (int x : new int[]{-81, -80, -65, -64, -1, 0, 15, 16, 31, 32, 79, 80}) {
            for (int z : new int[]{-225, -224, -193, -192, -161, -160, -129, -128, -65, -64, 79, 80}) {
                int y = LuoxiaTerrain.heightAt(x, z) - 4;
                BlockState expected = blueprint.sampleArchitecture(x, y, z);
                if (expected == null) expected = LuoxiaTerrain.blockAt(x, y, z);
                helper.assertTrue(local(level, x, y, z).getBlock() == expected.getBlock(),
                        "Chunk seam differs from the blueprint at " + x + "," + y + "," + z);
            }
        }
        System.out.println("Luoxia server test: live ascent, doors, interior, covered bridges, roof and chunk seams PASS");
    }

    private static BlockState local(ServerLevel level, int x, int y, int z) {
        return level.getBlockState(ORIGIN.offset(x, y, z));
    }

    private static void passage(GameTestHelper helper, ServerLevel level, int x, int feet, int z) {
        helper.assertTrue(!local(level, x, feet - 1, z).isAir()
                        && local(level, x, feet, z).isAir() && local(level, x, feet + 1, z).isAir(),
                "Live passage is blocked/unsupported at " + x + "," + feet + "," + z);
    }
}
