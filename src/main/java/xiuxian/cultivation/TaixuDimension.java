package xiuxian.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import xiuxian.network.XiuxianNetwork;

public final class TaixuDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("xiuxian", "taixu"));
    public static final double WORLD_SCALE = 8.0D;
    public static final float FLYING_SPEED = 0.12F;
    private static final int ENTRY_COST = 40;
    private static final String TRIP_SNAPSHOT = "xiuxian_taixu_cultivation";
    /** Durable fallback for capability instances recreated during a dimension transfer. */
    private static final String CULTIVATION_STATE = "xiuxian_cultivation_state";

    private TaixuDimension() {}

    public static boolean isTaixu(Level level) {
        return level.dimension().equals(LEVEL);
    }

    public static void toggle(ServerPlayer player) {
        // Always resolve through the durable state first. The capability on a
        // replacement player can be initialized while still containing the
        // default realm from before the transfer.
        CultivationData data = recoverTripData(player);
        if (isTaixu(player.level())) {
            returnToWorld(player, data);
            return;
        }
        // A dimension transfer can expose a freshly attached capability for a
        // short moment. Recover the live cultivation record before checking the
        // permanent Taixu requirement so a valid Purple Mansion cultivator is
        // never rejected because the capability was not copied yet.
        if (data == null || !data.isInitialized()) {
            data = recoverTripData(player);
        }
        if (data == null || !data.isInitialized()
                || data.realm().ordinal() < CultivationRealm.PURPLE_MANSION.ordinal()) {
            player.sendSystemMessage(Component.literal("唯有紫府真人，方可踏入或离开太虚。"));
            return;
        }
        if (data.isMeditating() || data.isStudyingTechnique() || player.isPassenger()) {
            player.sendSystemMessage(Component.literal("当前心神未定，不能行太虚。"));
            return;
        }
        ServerLevel taixu = player.server.getLevel(LEVEL);
        if (taixu == null) {
            player.sendSystemMessage(Component.literal("太虚界层尚未载入，请重进世界后再试。"));
            return;
        }
        int travelReserve = Math.max(80, Math.min(600, data.trueQiMaximum() / 40));
        int entryRequirement = ENTRY_COST + travelReserve;
        if (data.trueQi() < entryRequirement) {
            player.sendSystemMessage(Component.literal("入太虚需 40 点真炁，并至少留存 "
                    + travelReserve + " 点行旅余炁；当前真炁不足。"));
            return;
        }
        data.spendTrueQi(ENTRY_COST);

        data.setTaixuAnchor(player.level().dimension().location().toString(), player.getX(), player.getY(),
                player.getZ(), player.getYRot(), player.getXRot(), player.getAbilities().getFlyingSpeed(),
                player.getAbilities().mayfly, player.getAbilities().flying);
        saveTripSnapshot(player, data);
        CompoundTag tripSnapshot = data.serializeNBT();
        double taixuX = player.getX() / WORLD_SCALE;
        double taixuZ = player.getZ() / WORLD_SCALE;
        Entity transferred = player.changeDimension(taixu, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destination,
                    java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(new Vec3(taixuX, 120.0D, taixuZ), Vec3.ZERO,
                        player.getYRot(), player.getXRot());
            }
        });
        if (!(transferred instanceof ServerPlayer traveler)) {
            data.restoreTrueQi(ENTRY_COST);
            data.clearTaixuAnchor();
            clearTripSnapshot(player);
            XiuxianNetwork.syncCultivation(player, data);
            player.sendSystemMessage(Component.literal("太虚界门未能稳固，你仍留在现世。"));
            return;
        }

        CultivationData travelerData = recoverTripData(traveler, null, tripSnapshot);
        if (travelerData == null || !travelerData.isInitialized()) {
            // Dimension transfer may attach the new capability one tick later.
            // Keep the player in Taixu while the persistent snapshot is retried.
            traveler.getPersistentData().put(TRIP_SNAPSHOT, tripSnapshot.copy());
            traveler.getAbilities().mayfly = true;
            traveler.getAbilities().flying = true;
            traveler.getAbilities().setFlyingSpeed(FLYING_SPEED);
            traveler.onUpdateAbilities();
            buildArrivalPlatform(traveler.serverLevel(), BlockPos.containing(taixuX, 120.0D, taixuZ));
            traveler.sendSystemMessage(Component.literal("修行档案正在恢复，太虚行路已安全中止。"));
            return;
        }

        traveler.getAbilities().mayfly = true;
        traveler.getAbilities().flying = true;
        traveler.getAbilities().setFlyingSpeed(FLYING_SPEED);
        traveler.onUpdateAbilities();
        CultivationAttributeEffects.applyAndPreserveHealth(traveler, travelerData);
        CultivationAttributeEffects.sync(traveler);
        buildArrivalPlatform(traveler.serverLevel(), BlockPos.containing(taixuX, 120.0D, taixuZ));
        traveler.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                traveler.getX(), traveler.getY(), traveler.getZ(), 48, 1.0D, 0.8D, 1.0D, 0.02D);
        traveler.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.8F);
        traveler.sendSystemMessage(Component.literal("你踏入太虚。此间一里，现世八里；真炁将尽时会送你返世。"));
        XiuxianNetwork.syncCultivation(traveler, travelerData);
    }

    public static void returnToWorld(ServerPlayer player, CultivationData data) {
        returnToWorld(player, data, null);
    }

    private static void returnToWorld(ServerPlayer player, CultivationData data, CompoundTag fallbackSnapshot) {
        if (!isTaixu(player.level())) {
            return;
        }
        data = recoverTripData(player, data, fallbackSnapshot);
        CompoundTag tripSnapshot = data != null && data.isInitialized()
                ? data.serializeNBT() : fallbackSnapshot;
        if (tripSnapshot != null && tripSnapshot.getBoolean("initialized")) {
            player.getPersistentData().put(TRIP_SNAPSHOT, tripSnapshot.copy());
        }
        boolean hasAnchor = data != null && data.hasTaixuAnchor();
        ServerLevel overworld = player.server.overworld();
        ServerLevel destination = overworld;
        if (hasAnchor) {
            ResourceLocation originId = ResourceLocation.tryParse(data.taixuOriginDimension());
            if (originId != null) {
                ServerLevel anchoredLevel = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, originId));
                if (anchoredLevel != null) destination = anchoredLevel;
            }
        }

        double mappedX = player.getX() * WORLD_SCALE;
        double mappedZ = player.getZ() * WORLD_SCALE;
        double borderInset = 8.0D;
        mappedX = Math.max(destination.getWorldBorder().getMinX() + borderInset,
                Math.min(destination.getWorldBorder().getMaxX() - borderInset, mappedX));
        mappedZ = Math.max(destination.getWorldBorder().getMinZ() + borderInset,
                Math.min(destination.getWorldBorder().getMaxZ() - borderInset, mappedZ));
        BlockPos landing = findSafeLanding(destination, player, mappedX, mappedZ);
        boolean usedAnchorFallback = false;
        boolean usedSpawnFallback = false;
        if (landing == null && hasAnchor) {
            double anchorX = data.taixuOriginX();
            double anchorZ = data.taixuOriginZ();
            BlockPos anchorLanding = findSafeLanding(destination, player, anchorX, anchorZ);
            if (anchorLanding != null) {
                landing = anchorLanding;
                usedAnchorFallback = true;
            }
        }
        if (landing == null) {
            usedSpawnFallback = true;
            destination = overworld;
            BlockPos spawn = overworld.getSharedSpawnPos();
            landing = findSafeLanding(overworld, player, spawn.getX(), spawn.getZ());
            if (landing == null) {
                int spawnY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        spawn.getX(), spawn.getZ());
                landing = new BlockPos(spawn.getX(), Math.max(overworld.getMinBuildHeight() + 1, spawnY),
                        spawn.getZ());
            }
        }

        double x = landing.getX() + 0.5D;
        double y = landing.getY();
        double z = landing.getZ() + 0.5D;
        float yaw = hasAnchor ? data.taixuOriginYaw() : player.getYRot();
        float pitch = hasAnchor ? data.taixuOriginPitch() : player.getXRot();
        float flyingSpeed = hasAnchor ? data.taixuOriginFlyingSpeed() : 0.05F;
        boolean restoreMayfly = player.isCreative() || player.isSpectator()
                || (data != null && data.isInitialized()
                && data.realm().ordinal() >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal())
                || (hasAnchor && data.taixuOriginMayfly());
        boolean restoreFlying = (player.isCreative() || player.isSpectator())
                || (hasAnchor && data.taixuOriginFlying());

        ServerLevel returnLevel = destination;
        Entity transferred = player.changeDimension(returnLevel, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel level,
                    java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch);
            }
        });
        if (!(transferred instanceof ServerPlayer traveler)) {
            player.sendSystemMessage(Component.literal("返世界门未能稳固；再次使用 V 键或 /xiuxian return 重试。"));
            return;
        }

        CultivationData returnedData = recoverTripData(traveler, null, tripSnapshot);
        if (returnedData != null && returnedData.isInitialized()) {
            returnedData.clearTaixuAnchor();
            persistCultivationData(traveler, returnedData);
        }
        if (returnedData != null) XiuxianNetwork.syncCultivation(traveler, returnedData);
        traveler.getAbilities().mayfly = restoreMayfly;
        traveler.getAbilities().flying = restoreMayfly && restoreFlying;
        traveler.getAbilities().setFlyingSpeed(flyingSpeed);
        traveler.setNoGravity(false);
        traveler.onUpdateAbilities();
        if (returnedData != null && returnedData.isInitialized()) {
            CultivationAttributeEffects.applyAndPreserveHealth(traveler, returnedData);
            CultivationAttributeEffects.sync(traveler);
        }
        traveler.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                traveler.getX(), traveler.getY(), traveler.getZ(), 48, 1.0D, 0.8D, 1.0D, 0.02D);
        traveler.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.0F);
        traveler.sendSystemMessage(Component.literal(usedSpawnFallback
                ? "返世锚点或原落点不可用，你已被引回主世界出生地。"
                : usedAnchorFallback ? "落点不稳，你循返世锚点回到来处。"
                : "你沿太虚行过一程，已抵现世新的落点。"));
    }

    public static CultivationData recoverTripData(ServerPlayer player) {
        return recoverTripData(player, null, null);
    }

    private static CultivationData recoverTripData(ServerPlayer player, CultivationData current,
                                                    CompoundTag fallbackSnapshot) {
        CultivationData data = current;
        if (data == null) {
            data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
        }
        if (data == null) {
            player.reviveCaps();
            data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
        }
        // A snapshot explicitly supplied by the transfer flow is authoritative:
        // Forge may attach a fresh, initialized-but-stale capability to the new
        // player entity before the dimension callback runs.
        if (data != null && fallbackSnapshot != null && fallbackSnapshot.getBoolean("initialized")) {
            data.deserializeNBT(fallbackSnapshot.copy());
            savePersistentState(player, data);
            return data;
        }
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag snapshot = null;
        if (isTaixu(player.level())
                && persistentData.contains(TRIP_SNAPSHOT, Tag.TAG_COMPOUND)
                && persistentData.getCompound(TRIP_SNAPSHOT).getBoolean("initialized")) {
            snapshot = persistentData.getCompound(TRIP_SNAPSHOT);
        } else if (persistentData.contains(CULTIVATION_STATE, Tag.TAG_COMPOUND)
                && persistentData.getCompound(CULTIVATION_STATE).getBoolean("initialized")) {
            snapshot = persistentData.getCompound(CULTIVATION_STATE);
        } else if (persistentData.contains(TRIP_SNAPSHOT, Tag.TAG_COMPOUND)
                && persistentData.getCompound(TRIP_SNAPSHOT).getBoolean("initialized")) {
            // Keep the last valid Taixu record as a repair fallback. It is
            // overwritten on every trip and cleared only on death.
            snapshot = persistentData.getCompound(TRIP_SNAPSHOT);
        }

        // Realm progression is monotonic during normal play. If an initialized
        // capability survived a transfer with the default or an older realm,
        // prefer the durable copy.
        if (data != null && data.isInitialized()) {
            if (snapshot != null && isMoreAdvanced(snapshot, data)) {
                data.deserializeNBT(snapshot.copy());
            }
            savePersistentState(player, data);
            return data;
        }
        if (data != null && snapshot != null && snapshot.getBoolean("initialized")) {
            data.deserializeNBT(snapshot.copy());
        }
        if (data != null) {
            savePersistentState(player, data);
        }
        return data;
    }

    private static boolean isMoreAdvanced(CompoundTag snapshot, CultivationData data) {
        CultivationRealm savedRealm = CultivationRealm.byId(snapshot.getString("realm"));
        if (savedRealm == null) return false;
        int realmOrder = Integer.compare(savedRealm.ordinal(), data.realm().ordinal());
        return realmOrder > 0 || (realmOrder == 0
                && snapshot.getInt("realmLevel") > data.realmLevel());
    }

    private static void savePersistentState(ServerPlayer player, CultivationData data) {
        // A freshly cloned player can expose the default capability for a few
        // ticks. Never let that empty record overwrite a valid progression
        // snapshot from the previous entity.
        if (data != null && data.isInitialized()) {
            player.getPersistentData().put(CULTIVATION_STATE, data.serializeNBT());
        }
    }

    public static void persistCultivationData(ServerPlayer player, CultivationData data) {
        savePersistentState(player, data);
    }

    public static void saveTripSnapshot(ServerPlayer player, CultivationData data) {
        if (data.isInitialized()) {
            player.getPersistentData().put(TRIP_SNAPSHOT, data.serializeNBT());
        }
    }

    public static boolean hasValidTripSnapshot(ServerPlayer player) {
        return player.getPersistentData().contains(TRIP_SNAPSHOT, Tag.TAG_COMPOUND)
                && player.getPersistentData().getCompound(TRIP_SNAPSHOT).getBoolean("initialized");
    }

    public static void clearTripSnapshot(ServerPlayer player) {
        player.getPersistentData().remove(TRIP_SNAPSHOT);
    }

    public static void onDimensionChanged(ServerPlayer player) {
        boolean hasTripSnapshot = player.getPersistentData().contains(TRIP_SNAPSHOT, Tag.TAG_COMPOUND);
        if (!isTaixu(player.level())) {
            // Recover from the durable state if Forge attached a fresh capability
            // during the transfer. This callback is also the final authority for
            // restoring attributes before the client renders the new dimension.
            CultivationData data = recoverTripData(player);
            if (data != null && data.isInitialized()) {
                CultivationAttributeEffects.applyAndPreserveHealth(player, data);
                CultivationAttributeEffects.sync(player);
            }
            if (data != null) XiuxianNetwork.syncCultivation(player, data);
            return;
        }

        CompoundTag snapshot = hasTripSnapshot
                ? player.getPersistentData().getCompound(TRIP_SNAPSHOT).copy() : null;
        CultivationData data = recoverTripData(player, null, snapshot);
        if (data == null || !data.isInitialized()) return;
        if (isTaixu(player.level())) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlyingSpeed(FLYING_SPEED);
            player.onUpdateAbilities();
            CultivationAttributeEffects.applyAndPreserveHealth(player, data);
            if (player.tickCount % 20 == 0) {
                saveTripSnapshot(player, data);
            }
        }
        XiuxianNetwork.syncCultivation(player, data);
    }

    public static void ensureTaixuState(ServerPlayer player, CultivationData data) {
        if (!isTaixu(player.level()) || data == null || !data.isInitialized()) return;
        if (!player.getAbilities().mayfly || !player.getAbilities().flying
                || Math.abs(player.getAbilities().getFlyingSpeed() - FLYING_SPEED) > 0.0001F) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlyingSpeed(FLYING_SPEED);
            player.onUpdateAbilities();
        }
        player.setNoGravity(false);
        CultivationAttributeEffects.applyAndPreserveHealth(player, data);
    }

    private static void buildArrivalPlatform(ServerLevel level, BlockPos center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 10) continue;
                BlockPos floor = center.below().offset(dx, 0, dz);
                net.minecraft.world.level.block.state.BlockState state = dx == 0 && dz == 0
                        ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                        : Math.abs(dx) == 3 || Math.abs(dz) == 3
                        ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
                level.setBlock(floor, state, 3);
                level.setBlock(floor.above(), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(floor.above(2), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (BlockPos offset : new BlockPos[]{new BlockPos(2, 0, 0), new BlockPos(-2, 0, 0),
                new BlockPos(0, 0, 2), new BlockPos(0, 0, -2)}) {
            BlockPos pillar = center.offset(offset);
            level.setBlock(pillar, Blocks.CALCITE.defaultBlockState(), 3);
            level.setBlock(pillar.above(), Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            level.setBlock(pillar.above(2), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }
    }

    private static BlockPos findSafeLanding(ServerLevel level, ServerPlayer player, double centerX,
                                            double centerZ) {
        int baseX = net.minecraft.util.Mth.floor(centerX);
        int baseZ = net.minecraft.util.Mth.floor(centerZ);
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, level.getMinBuildHeight(), z))) continue;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (y <= level.getMinBuildHeight() || y + 2 >= level.getMaxBuildHeight()) continue;
                    BlockPos feet = new BlockPos(x, y, z);
                    if (!level.getBlockState(feet.below()).blocksMotion()
                            || !level.getFluidState(feet).isEmpty()
                            || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                    double moveX = x + 0.5D - player.getX();
                    double moveY = y - player.getY();
                    double moveZ = z + 0.5D - player.getZ();
                    if (level.noCollision(player, player.getBoundingBox().move(moveX, moveY, moveZ))) {
                        return feet;
                    }
                }
            }
        }
        return null;
    }
}
