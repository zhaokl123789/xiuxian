package xiuxian.sect;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "xiuxian")
public final class LuoxiaSectEvents {
    private LuoxiaSectEvents() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiuxian").then(Commands.literal("luoxia")
                .executes(ctx -> status(ctx.getSource()))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("locate").executes(ctx -> locate(ctx.getSource())))
                .then(Commands.literal("plan").requires(s -> s.hasPermission(2))
                        .executes(ctx -> plan(ctx.getSource(), ctx.getSource().getPlayerOrException().blockPosition().below()))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> plan(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos")))))
                .then(Commands.literal("build").requires(s -> s.hasPermission(2)).executes(ctx -> build(ctx.getSource())))
                .then(Commands.literal("pause").requires(s -> s.hasPermission(2)).executes(ctx -> pause(ctx.getSource())))
                .then(Commands.literal("resume").requires(s -> s.hasPermission(2)).executes(ctx -> resume(ctx.getSource())))
                .then(Commands.literal("visit").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("entrance").executes(ctx -> visit(ctx.getSource(), 0, 1, 108)))
                        .then(Commands.literal("court").executes(ctx -> visit(ctx.getSource(), 0, 85, -103)))
                        .then(Commands.literal("summit").executes(ctx -> visit(ctx.getSource(), 0, 134, -160))))));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) LuoxiaConstruction.tick(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        LuoxiaConstruction.release(event.getServer().overworld());
    }

    private static int plan(CommandSourceStack source, BlockPos origin) {
        ServerLevel level = source.getServer().overworld();
        if (source.getLevel().dimension() != Level.OVERWORLD) return fail(source, "落霞宗外部只能在现世选址。");
        LuoxiaSiteData data = LuoxiaSiteData.get(level);
        if (data.origin != null && data.phase != LuoxiaSiteData.Phase.PLANNED
                && !(data.phase == LuoxiaSiteData.Phase.SURVEY && data.changedBlocks == 0)) {
            return fail(source, "此存档已经有落霞宗施工记录。可定位、暂停或续建，不会重复覆盖建造。");
        }
        if (origin.getY() + LuoxiaBlueprint.MIN_Y < level.getMinBuildHeight()
                || origin.getY() + LuoxiaBlueprint.MAX_Y >= level.getMaxBuildHeight()) {
            return fail(source, "选址超出世界高度。现世基准层 Y 须在 -52 至 97 之间，建议选择低地 Y=64。");
        }
        for (int x : new int[] {LuoxiaBlueprint.MIN_X, LuoxiaBlueprint.MAX_X}) {
            for (int z : new int[] {LuoxiaBlueprint.MIN_Z, LuoxiaBlueprint.MAX_Z}) {
                if (!level.getWorldBorder().isWithinBounds(origin.offset(x, 0, z))) {
                    return fail(source, "建筑范围超出世界边界，请重新选址。");
                }
            }
        }
        LuoxiaConstruction.release(level);
        data.origin = origin.immutable();
        data.phase = LuoxiaSiteData.Phase.PLANNED;
        data.paused = false;
        data.problem = "";
        data.version = LuoxiaBlueprint.VERSION;
        data.chunkIndex = data.operationIndex = 0;
        data.cellIndex = data.changedBlocks = 0;
        data.setDirty();
        String from = LuoxiaConstruction.coordinates(origin.offset(LuoxiaBlueprint.MIN_X, LuoxiaBlueprint.MIN_Y, LuoxiaBlueprint.MIN_Z));
        String to = LuoxiaConstruction.coordinates(origin.offset(LuoxiaBlueprint.MAX_X, LuoxiaBlueprint.MAX_Y, LuoxiaBlueprint.MAX_Z));
        source.sendSuccess(() -> Component.literal("落霞宗选址已记录，面朝北方。范围：" + from + " 至 " + to
                + "，约 " + LuoxiaConstruction.chunkCount(origin) + " 个区块。\n"
                + "执行 /xiuxian luoxia build 将替换范围内地形。请在专用测试存档或无建筑的低地施工，离开施工范围。"
                + "建造前会检查箱子等方块实体；可在开工前再次 plan 调整位置。"), true);
        return 1;
    }

    private static int build(CommandSourceStack source) {
        LuoxiaSiteData data = LuoxiaSiteData.get(source.getServer().overworld());
        if (data.origin == null) return fail(source, "请先用 /xiuxian luoxia plan x y z 选址并查看替换范围。");
        if (data.phase != LuoxiaSiteData.Phase.PLANNED) return fail(source, "此处已有施工记录。暂停后使用 resume 续建。");
        if (data.paused || !data.problem.isEmpty() || data.version != LuoxiaBlueprint.VERSION || !data.validCursor()) {
            return fail(source, "\u9009\u5740\u8bb0\u5f55\u5f02\u5e38\u6216\u7248\u672c\u4e0d\u5339\u914d\uff0c\u8bf7\u91cd\u65b0 plan \u540e\u518d\u5f00\u5de5\u3002");
        }
        data.phase = LuoxiaSiteData.Phase.SURVEY;
        data.setDirty();
        source.sendSuccess(() -> Component.literal("落霞宗开始检查场地，随后分批塑山、建殿和引水。"
                + "请离开施工范围，可用 status 查看进度、pause 暂停。"), true);
        return 1;
    }

    private static int pause(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        LuoxiaSiteData data = LuoxiaSiteData.get(level);
        if (!data.active()) return fail(source, "当前没有运行中的落霞宗施工。");
        data.paused = true;
        data.setDirty();
        LuoxiaConstruction.release(level);
        source.sendSuccess(() -> Component.literal("施工已暂停并记录位置，可用 resume 继续。"), true);
        return 1;
    }

    private static int resume(CommandSourceStack source) {
        LuoxiaSiteData data = LuoxiaSiteData.get(source.getServer().overworld());
        if (!data.paused || data.origin == null) return fail(source, "没有需要续建的落霞宗施工。");
        if (data.phase == LuoxiaSiteData.Phase.PLANNED || data.phase == LuoxiaSiteData.Phase.COMPLETE) {
            return fail(source, "当前施工记录不处于可续建阶段。");
        }
        if (data.version != LuoxiaBlueprint.VERSION) return fail(source, "施工记录与建筑版本不一致，不能直接续建。");
        if (!data.validCursor()) return fail(source, "施工游标无效，不能直接续建，请检查存档备份。");
        data.paused = false;
        data.problem = "";
        data.setDirty();
        source.sendSuccess(() -> Component.literal("落霞宗施工已恢复，将从保存的位置继续。"), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        LuoxiaSiteData data = LuoxiaSiteData.get(source.getServer().overworld());
        if (data.origin == null) {
            source.sendSuccess(() -> Component.literal("现世尚未建设落霞宗。管理员可用 /xiuxian luoxia plan x y z 选址。"), false);
            return 1;
        }
        String stage = switch (data.phase) {
            case PLANNED -> "已选址，等待开工";
            case SURVEY -> "检查场地";
            case TERRAIN -> "塑造落霞山";
            case BUILDINGS -> "营建殿堂、廊桥与林木";
            case WATER -> "引水成湖与瀑布";
            case COMPLETE -> "外部建造完成";
        };
        source.sendSuccess(() -> Component.literal("落霞宗：" + stage + (data.paused ? "（已暂停）" : "")
                + "。基准：" + LuoxiaConstruction.coordinates(data.origin)
                + (data.phase == LuoxiaSiteData.Phase.PLANNED || data.phase == LuoxiaSiteData.Phase.COMPLETE ? ""
                : "。当前阶段区块：" + data.chunkIndex + "/" + LuoxiaConstruction.chunkCount(data.origin))
                + "。已更新 " + data.changedBlocks + " 格。" + (data.problem.isEmpty() ? "" : "\n" + data.problem)), false);
        return 1;
    }

    private static int locate(CommandSourceStack source) {
        LuoxiaSiteData data = LuoxiaSiteData.get(source.getServer().overworld());
        if (data.origin == null) return fail(source, "此存档还没有落霞宗选址。");
        source.sendSuccess(() -> Component.literal("落霞宗位于现世，登山入口："
                + LuoxiaConstruction.coordinates(data.origin.offset(0, 1, 108)) + "。"
                + (data.phase == LuoxiaSiteData.Phase.COMPLETE ? "" : "建筑尚未完成。")), false);
        return 1;
    }

    private static int visit(CommandSourceStack source, int x, int y, int z) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isCreative() && !player.isSpectator()) return fail(source, "观察传送仅供创造或旁观模式测试使用。");
        ServerLevel level = source.getServer().overworld();
        LuoxiaSiteData data = LuoxiaSiteData.get(level);
        if (data.origin == null || data.phase != LuoxiaSiteData.Phase.COMPLETE) return fail(source, "请等待外部建筑完成后再前往观察点。");
        BlockPos target = data.origin.offset(x, y, z);
        player.teleportTo(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 180, 0);
        return 1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
