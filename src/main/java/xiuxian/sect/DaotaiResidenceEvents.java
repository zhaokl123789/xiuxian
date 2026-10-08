package xiuxian.sect;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Command, tick and mob-protection entry points for the standalone residence. */
@Mod.EventBusSubscriber(modid = "xiuxian")
public final class DaotaiResidenceEvents {
    private DaotaiResidenceEvents() {}

    public static boolean generate(ServerPlayer player, BlockPos origin) {
        if (!player.isCreative() || !player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Dao-Tai residence inspection requires creative operator permission."));
            return false;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.literal("The standalone residence can only be built in the Overworld."));
            return false;
        }
        int highestOrigin = player.serverLevel().getMaxBuildHeight() - 1 - DaotaiResidenceGenerator.MAX_Y;
        if (origin.getY() > highestOrigin) {
            player.sendSystemMessage(Component.literal("The Dao-Tai palace rises 222 blocks above its origin. Choose an origin at Y <= "
                    + highestOrigin + " (for example: /xiuxian daotai generate ~ 64 ~)."));
            return false;
        }
        if (!DaotaiResidenceGenerator.generate(player.serverLevel(), origin)) {
            player.sendSystemMessage(Component.literal("The site is outside the world border, overlaps an existing residence, or has an invalid height."));
            return false;
        }
        player.sendSystemMessage(Component.literal("Dao-Tai residence construction queued at " + origin.toShortString()
                + ". Use /xiuxian daotai status for progress."));
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && (level.dimension() == Level.OVERWORLD || level.dimension() == LuoxiaInnerDimension.LEVEL))
            DaotaiResidenceConstruction.tick(level);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        event.getServer().getAllLevels().forEach(DaotaiResidenceConstruction::release);
    }

    @SubscribeEvent
    public static void naturalMonsterSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL
                || event.getEntity().getType().getCategory() != MobCategory.MONSTER) return;
        ServerLevel level = event.getLevel().getLevel();
        if (level.dimension() != Level.OVERWORLD) return;
        DaotaiResidenceConstruction.Data data = DaotaiResidenceConstruction.data(level);
        if (data.origin != null && inside(data.origin, event.getEntity().blockPosition())) event.setResult(
                net.minecraftforge.eventbus.api.Event.Result.DENY);
        else for (long originLong : data.origins) {
            if (inside(BlockPos.of(originLong), event.getEntity().blockPosition())) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiuxian").then(Commands.literal("daotai")
                .then(Commands.literal("inner")
                        .then(Commands.literal("status").executes(context -> {
                            ServerLevel level = context.getSource().getServer().getLevel(LuoxiaInnerDimension.LEVEL);
                            if (level == null) {
                                context.getSource().sendFailure(Component.literal("落霞洞天尚未加载。"));
                                return 0;
                            }
                            LuoxiaInnerRealmGenerator.ensureGenerated(level);
                            context.getSource().sendSuccess(() -> Component.literal(
                                    DaotaiResidenceConstruction.status(level)), false);
                            return 1;
                        }))
                        .then(Commands.literal("visit").requires(source -> source.hasPermission(2))
                                .then(Commands.literal("entrance").executes(context -> visit(context.getSource(), "entrance", true)))
                                .then(Commands.literal("palace").executes(context -> visit(context.getSource(), "palace", true)))
                                .then(Commands.literal("upper").executes(context -> visit(context.getSource(), "upper", true)))
                                .then(Commands.literal("view").executes(context -> visit(context.getSource(), "view", true)))))
                .then(Commands.literal("generate").requires(source -> source.hasPermission(2))
                        .executes(context -> generateCommand(context.getSource(), null))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> generateCommand(context.getSource(),
                                        BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("status").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(DaotaiResidenceConstruction.status(
                            context.getSource().getServer().overworld())), false);
                    return 1;
                }))
                .then(Commands.literal("rebuild").requires(source -> source.hasPermission(2)).executes(context -> {
                    var data = DaotaiResidenceConstruction.data(context.getSource().getServer().overworld());
                    if (data.origin != null || data.origins.isEmpty()) return 0;
                    long last = data.origins.remove(data.origins.size() - 1);
                    boolean queued = false;
                    try { queued = generate(context.getSource().getPlayerOrException(), BlockPos.of(last)); }
                    finally { if (!queued) data.origins.add(last); data.setDirty(); }
                    return queued ? 1 : 0;
                }))
                .then(Commands.literal("visit").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("entrance").executes(context -> visit(context.getSource(), "entrance")))
                        .then(Commands.literal("palace").executes(context -> visit(context.getSource(), "palace")))
                        .then(Commands.literal("upper").executes(context -> visit(context.getSource(), "upper")))
                        .then(Commands.literal("view").executes(context -> visit(context.getSource(), "view"))))
                .then(Commands.literal("cancel").requires(source -> source.hasPermission(2)).executes(context -> {
                    DaotaiResidenceConstruction.cancel(context.getSource().getServer().overworld());
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Dao-Tai residence construction cancelled; already-written blocks remain for inspection."), false);
                    return 1;
                }))));
    }

    private static int visit(CommandSourceStack source, String point) throws CommandSyntaxException {
        return visit(source, point, false);
    }

    private static int visit(CommandSourceStack source, String point, boolean inner) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isCreative()) {
            source.sendFailure(Component.literal("Dao-Tai inspection visits require creative mode."));
            return 0;
        }
        ServerLevel level = inner ? source.getServer().getLevel(LuoxiaInnerDimension.LEVEL) : source.getServer().overworld();
        if (level == null) {
            source.sendFailure(Component.literal("落霞洞天尚未加载。"));
            return 0;
        }
        if (inner) LuoxiaInnerRealmGenerator.ensureGenerated(level);
        var data = DaotaiResidenceConstruction.data(level);
        if (inner ? !LuoxiaDaotaiResidence.isReady(level) : data.origins.isEmpty()) {
            source.sendFailure(Component.literal("Wait for Dao-Tai construction to finish before visiting."));
            return 0;
        }
        // Enter through the transport boundary so the player's return anchor is preserved.
        if (inner && !LuoxiaInnerDimension.isInner(player.level())) {
            var id = player.getUUID();
            if (!LuoxiaInnerDimension.enter(player)) return 0;
            player = source.getServer().getPlayerList().getPlayer(id);
            if (player == null) return 0;
        }
        BlockPos origin = inner ? LuoxiaDaotaiResidence.ORIGIN : BlockPos.of(data.origins.get(data.origins.size() - 1));
        BlockPos target = switch (point) {
            case "palace" -> origin.offset(0, 75, -75);
            case "upper" -> origin.offset(0, 144, -92);
            case "view" -> origin.offset(0, 155, 260);
            default -> origin.offset(0, 2, 209);
        };
        if (point.equals("view")) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        player.teleportTo(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 180, 0);
        player.fallDistance = 0;
        return 1;
    }

    private static int generateCommand(CommandSourceStack source, BlockPos explicitOrigin) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return generate(player, explicitOrigin == null ? player.blockPosition().below() : explicitOrigin) ? 1 : 0;
        } catch (CommandSyntaxException failure) {
            source.sendFailure(Component.literal("Run the Dao-Tai generation command as a player."));
            return 0;
        }
    }

    private static boolean inside(BlockPos origin, BlockPos pos) {
        return pos.getX() >= origin.getX() + DaotaiResidenceGenerator.MIN_X
                && pos.getX() <= origin.getX() + DaotaiResidenceGenerator.MAX_X
                && pos.getY() >= origin.getY() + DaotaiResidenceGenerator.MIN_Y
                && pos.getY() <= origin.getY() + DaotaiResidenceGenerator.MAX_Y
                && pos.getZ() >= origin.getZ() + DaotaiResidenceGenerator.MIN_Z
                && pos.getZ() <= origin.getZ() + DaotaiResidenceGenerator.MAX_Z;
    }
}
