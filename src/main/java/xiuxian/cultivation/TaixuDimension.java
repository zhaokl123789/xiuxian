package xiuxian.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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

    private TaixuDimension() {}

    public static boolean isTaixu(Level level) {
        return level.dimension().equals(LEVEL);
    }

    public static void toggle(ServerPlayer player) {
        CultivationData data = player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
        if (data == null || !data.isInitialized() || data.realm() != CultivationRealm.PURPLE_MANSION) {
            player.sendSystemMessage(Component.literal("唯有紫府真人，方可踏入或离开太虚。"));
            return;
        }
        if (data.isMeditating() || data.isStudyingTechnique() || player.isPassenger()) {
            player.sendSystemMessage(Component.literal("当前心神未定，不能行太虚。"));
            return;
        }
        if (isTaixu(player.level())) {
            returnToWorld(player, data);
            return;
        }
        if (!player.level().dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.literal("太虚行路暂只可从主世界启程。"));
            return;
        }
        ServerLevel taixu = player.server.getLevel(LEVEL);
        if (taixu == null) {
            player.sendSystemMessage(Component.literal("太虚界层尚未载入，请重进世界后再试。"));
            return;
        }
        if (!data.spendTrueQi(ENTRY_COST)) {
            player.sendSystemMessage(Component.literal("入太虚需 40 点真炁，当前真炁不足。"));
            return;
        }

        data.setTaixuAnchor(player.level().dimension().location().toString(), player.getX(), player.getY(),
                player.getZ(), player.getYRot(), player.getXRot(), player.getAbilities().getFlyingSpeed());
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
            XiuxianNetwork.syncCultivation(player, data);
            player.sendSystemMessage(Component.literal("太虚界门未能稳固，你仍留在现世。"));
            return;
        }

        traveler.getAbilities().mayfly = true;
        traveler.getAbilities().flying = true;
        traveler.getAbilities().setFlyingSpeed(FLYING_SPEED);
        traveler.onUpdateAbilities();
        traveler.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                traveler.getX(), traveler.getY(), traveler.getZ(), 48, 1.0D, 0.8D, 1.0D, 0.02D);
        traveler.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.8F);
        traveler.sendSystemMessage(Component.literal("你踏入太虚。此间一里，现世八里；真炁将尽时会送你返世。"));
        XiuxianNetwork.syncCultivation(traveler, data);
    }

    public static void returnToWorld(ServerPlayer player, CultivationData data) {
        if (!data.hasTaixuAnchor()) {
            player.sendSystemMessage(Component.literal("返世锚点已失，无法确认太虚来处。"));
            return;
        }
        ResourceLocation originId = ResourceLocation.tryParse(data.taixuOriginDimension());
        ResourceKey<Level> originKey = originId == null ? Level.OVERWORLD
                : ResourceKey.create(Registries.DIMENSION, originId);
        ServerLevel destination = player.server.getLevel(originKey);
        if (destination == null) destination = player.server.overworld();

        double mappedX = player.getX() * WORLD_SCALE;
        double mappedZ = player.getZ() * WORLD_SCALE;
        double borderInset = 8.0D;
        mappedX = Math.max(destination.getWorldBorder().getMinX() + borderInset,
                Math.min(destination.getWorldBorder().getMaxX() - borderInset, mappedX));
        mappedZ = Math.max(destination.getWorldBorder().getMinZ() + borderInset,
                Math.min(destination.getWorldBorder().getMaxZ() - borderInset, mappedZ));
        BlockPos landing = findSafeLanding(destination, player, mappedX, mappedZ);
        boolean usedOriginFallback = landing == null;
        double x = landing == null ? data.taixuOriginX() : landing.getX() + 0.5D;
        double y = landing == null ? data.taixuOriginY() : landing.getY();
        double z = landing == null ? data.taixuOriginZ() : landing.getZ() + 0.5D;
        float yaw = data.taixuOriginYaw();
        float pitch = data.taixuOriginPitch();
        float flyingSpeed = data.taixuOriginFlyingSpeed();
        player.teleportTo(destination, x, y, z, yaw, pitch);
        data.clearTaixuAnchor();
        player.getAbilities().setFlyingSpeed(flyingSpeed);
        player.setNoGravity(false);
        player.onUpdateAbilities();
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY(), player.getZ(), 48, 1.0D, 0.8D, 1.0D, 0.02D);
        player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.0F);
        player.sendSystemMessage(Component.literal(usedOriginFallback
                ? "落点无处安身，你循返世锚点回到来处。"
                : "你沿太虚行过一程，已抵现世新的落点。"));
        XiuxianNetwork.syncCultivation(player, data);
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
