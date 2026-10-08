package xiuxian.sect;

import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.ITeleporter;

/**
 * The dedicated Luoxia sect inner-realm transport boundary.
 *
 * <p>The dimension itself is data-pack driven (see
 * {@code data/xiuxian/dimension/luoxia_inner.json}); this class owns the
 * player-facing entry/return contract so it remains independent from the
 * existing Taixu cultivation-space state machine.</p>
 */
@Mod.EventBusSubscriber(modid = "xiuxian")
public final class LuoxiaInnerDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("xiuxian", "luoxia_inner"));

    /** Floating arrival gate reserved for the first generation pass. */
    public static final BlockPos ENTRY = new BlockPos(0, 72, 0);
    /** Summit point on the exterior structure that opens the gate. */
    public static final int SUMMIT_Y = 134;
    public static final int SUMMIT_Z = -160;
    /** North terrace face of the older scenic arch, also a valid approach. */
    public static final int SCENIC_GATE_Z = -220;

    private static final String RETURN_DATA = "xiuxian_luoxia_inner_return";
    private static final int ENTRY_RADIUS_SQUARED = 64;

    private LuoxiaInnerDimension() {}

    public static boolean isInner(Level level) {
        return level != null && level.dimension().equals(LEVEL);
    }

    /**
     * Returns whether a world position is inside the visible summit gate's
     * interaction zone.  Keeping this predicate next to the transport code
     * prevents the visual gate and the event handler from drifting apart as
     * the exterior blueprint evolves.
     */
    static boolean isSummitGate(BlockPos origin, BlockPos position) {
        if (origin == null || position == null) return false;
        return position.distSqr(origin.offset(0, SUMMIT_Y, SUMMIT_Z)) <= ENTRY_RADIUS_SQUARED
                || position.distSqr(origin.offset(0, SUMMIT_Y, SCENIC_GATE_Z)) <= ENTRY_RADIUS_SQUARED;
    }

    /**
     * Opens the summit gate for a player standing at the completed exterior
     * site. The return anchor is written before the transfer and therefore
     * survives Forge's replacement ServerPlayer during a dimension change.
     */
    public static boolean enterFromSummit(ServerPlayer player) {
        if (isInner(player.level())) {
            player.sendSystemMessage(Component.literal("你已经身在落霞洞天之中。"));
            return false;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.literal("落霞洞天的入口只在落霞宗外部山巅开启。"));
            return false;
        }
        ServerLevel overworld = player.server.overworld();
        LuoxiaSiteData site = LuoxiaSiteData.get(overworld);
        if (site.origin == null || site.phase != LuoxiaSiteData.Phase.COMPLETE) {
            player.sendSystemMessage(Component.literal("落霞宗外部尚未完成，洞天入口暂未显现。"));
            return false;
        }
        if (!isSummitGate(site.origin, player.blockPosition())
                && !player.isCreative() && !player.isSpectator()) {
            player.sendSystemMessage(Component.literal("请从落霞宗最上方的天门进入洞天。"));
            return false;
        }
        // Old COMPLETE saves may still be processing their additive detail
        // cursor. Materialize the gate before transfer so the visible entry
        // is present immediately at the interaction point.
        LuoxiaConstruction.ensureSummitGate(overworld, site.origin);
        return enter(player);
    }

    /** Enter directly after a caller has already performed its own gate check. */
    public static boolean enter(ServerPlayer player) {
        if (isInner(player.level())) return false;
        ServerLevel destination = player.server.getLevel(LEVEL);
        if (destination == null) {
            player.sendSystemMessage(Component.literal("落霞洞天尚未加载，请重新进入世界后再试。"));
            return false;
        }
        // The realm has a deterministic, dimension-local first-load pass.  It
        // is safe to call here as well as from LevelEvent.Load: a player may
        // arrive during the same tick in which the dimension is first created.
        LuoxiaInnerRealmGenerator.ensureGenerated(destination);

        CompoundTag anchor = new CompoundTag();
        anchor.putString("dimension", player.level().dimension().location().toString());
        anchor.putDouble("x", player.getX());
        anchor.putDouble("y", player.getY());
        anchor.putDouble("z", player.getZ());
        anchor.putFloat("yaw", player.getYRot());
        anchor.putFloat("pitch", player.getXRot());
        anchor.putFloat("flyingSpeed", player.getAbilities().getFlyingSpeed());
        anchor.putBoolean("mayfly", player.getAbilities().mayfly);
        anchor.putBoolean("flying", player.getAbilities().flying);
        player.getPersistentData().put(RETURN_DATA, anchor);

        Entity transferred = player.changeDimension(destination, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel level,
                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(new Vec3(ENTRY.getX() + 0.5D, ENTRY.getY(), ENTRY.getZ() + 0.5D),
                        Vec3.ZERO, player.getYRot(), player.getXRot());
            }
        });
        if (!(transferred instanceof ServerPlayer traveler)) {
            player.getPersistentData().remove(RETURN_DATA);
            player.sendSystemMessage(Component.literal("洞天入口未能稳定，请稍后再试。"));
            return false;
        }

        buildArrivalPlatform(traveler.serverLevel(), ENTRY);
        traveler.sendSystemMessage(Component.literal("落霞天门开启，你已进入洞天福地·九重云阙。"));
        return true;
    }

    /** Return to the exact dimension and position recorded on entry. */
    public static boolean returnToOrigin(ServerPlayer player) {
        if (!isInner(player.level())) {
            player.sendSystemMessage(Component.literal("你当前不在落霞洞天之中。"));
            return false;
        }

        CompoundTag anchor = player.getPersistentData().getCompound(RETURN_DATA);
        ServerLevel destination = resolveDestination(player, anchor);
        double targetX = anchor.contains("x", Tag.TAG_DOUBLE) ? anchor.getDouble("x") : destination.getSharedSpawnPos().getX();
        double targetZ = anchor.contains("z", Tag.TAG_DOUBLE) ? anchor.getDouble("z") : destination.getSharedSpawnPos().getZ();
        BlockPos landing = findSafeLanding(destination, targetX, targetZ);
        if (landing == null) {
            BlockPos spawn = destination.getSharedSpawnPos();
            landing = findSafeLanding(destination, spawn.getX(), spawn.getZ());
        }
        if (landing == null) {
            player.sendSystemMessage(Component.literal("外部落点暂不可用，洞天出口保持开启。"));
            return false;
        }

        final BlockPos target = landing;
        final float yaw = anchor.contains("yaw", Tag.TAG_FLOAT) ? anchor.getFloat("yaw") : player.getYRot();
        final float pitch = anchor.contains("pitch", Tag.TAG_FLOAT) ? anchor.getFloat("pitch") : player.getXRot();
        Entity transferred = player.changeDimension(destination, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel level,
                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(new Vec3(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D),
                        Vec3.ZERO, yaw, pitch);
            }
        });
        if (!(transferred instanceof ServerPlayer traveler)) {
            player.sendSystemMessage(Component.literal("洞天出口未能稳定，请稍后再试。"));
            return false;
        }

        restoreAbilities(traveler, anchor);
        traveler.getPersistentData().remove(RETURN_DATA);
        traveler.sendSystemMessage(Component.literal("你沿着落霞天门返回了外部宗门。"));
        return true;
    }

    @Nullable
    private static ServerLevel resolveDestination(ServerPlayer player, CompoundTag anchor) {
        ResourceLocation id = ResourceLocation.tryParse(anchor.getString("dimension"));
        if (id != null) {
            ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level != null) return level;
        }
        return player.server.overworld();
    }

    private static void restoreAbilities(ServerPlayer player, CompoundTag anchor) {
        boolean mayfly = anchor.contains("mayfly", Tag.TAG_BYTE)
                ? anchor.getBoolean("mayfly") : player.isCreative() || player.isSpectator();
        boolean flying = anchor.contains("flying", Tag.TAG_BYTE) && anchor.getBoolean("flying");
        float speed = anchor.contains("flyingSpeed", Tag.TAG_FLOAT)
                ? anchor.getFloat("flyingSpeed") : 0.05F;
        player.getAbilities().mayfly = player.isCreative() || player.isSpectator() || mayfly;
        player.getAbilities().flying = player.getAbilities().mayfly && flying;
        player.getAbilities().setFlyingSpeed(speed);
        player.setNoGravity(false);
        player.onUpdateAbilities();
    }

    private static void buildArrivalPlatform(ServerLevel level, BlockPos center) {
        // Arrival geometry now belongs to the versioned inner-realm generator.
        // Do not rebuild it on every teleport: players are allowed to decorate
        // or replace the landing pad after the first visit.
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
    }

    private static BlockPos findSafeLanding(ServerLevel level, double centerX, double centerZ) {
        int baseX = net.minecraft.util.Mth.floor(centerX);
        int baseZ = net.minecraft.util.Mth.floor(centerZ);
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (y <= level.getMinBuildHeight() || y + 2 >= level.getMaxBuildHeight()) continue;
                    BlockPos feet = new BlockPos(x, y, z);
                    if (!level.getBlockState(feet.below()).blocksMotion()
                            || !level.getFluidState(feet).isEmpty()
                            || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                    return feet;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onSummitInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().dimension() != Level.OVERWORLD) return;
        LuoxiaSiteData site = LuoxiaSiteData.get(player.server.overworld());
        if (site.origin == null || site.phase != LuoxiaSiteData.Phase.COMPLETE) return;
        if (isSummitGate(site.origin, event.getPos()) && enterFromSummit(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void naturalMonsterSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL
                && event.getEntity().getType().getCategory() == MobCategory.MONSTER
                && isInner(event.getLevel().getLevel())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isInner(player.level())) {
            buildArrivalPlatform(player.serverLevel(), ENTRY);
        } else if (event.getFrom().equals(LEVEL)) {
            CompoundTag anchor = player.getPersistentData().getCompound(RETURN_DATA);
            if (!anchor.isEmpty()) restoreAbilities(player, anchor);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getOriginal() instanceof ServerPlayer original
                && isInner(original.level()) && event.getEntity() instanceof ServerPlayer replacement) {
            replacement.getPersistentData().remove(RETURN_DATA);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !isInner(player.level())) {
            // A death inside the realm must not leave a stale exit anchor on
            // the newly spawned player, even if another clone listener copied
            // the original persistent data after our Clone callback ran.
            player.getPersistentData().remove(RETURN_DATA);
        }
    }

    /** Command helper used by LuoxiaSectEvents. */
    public static int enterCommand(ServerPlayer player) throws CommandSyntaxException {
        return enterFromSummit(player) ? 1 : 0;
    }

    /** Command helper used by LuoxiaSectEvents. */
    public static int returnCommand(ServerPlayer player) throws CommandSyntaxException {
        return returnToOrigin(player) ? 1 : 0;
    }
}
