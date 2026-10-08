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
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Item and command entry points for the standalone inspection build. */
@Mod.EventBusSubscriber(modid = "xiuxian")
public final class MortalTownEvents {
    private MortalTownEvents() {}

    public static boolean generate(ServerPlayer player, BlockPos origin) {
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.literal("凡人城镇验收道具仅供创造模式使用。"));
            return false;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.literal("请在主世界验收城镇，暂不接入洞天随机生成。"));
            return false;
        }
        if (!MortalTownGenerator.generate(player.serverLevel(), origin)) {
            player.sendSystemMessage(Component.literal("未开始施工：选址越界、该原点已有城镇，或已有施工任务。"
                    + MortalTownConstruction.status(player.serverLevel())));
            return false;
        }
        player.sendSystemMessage(Component.literal("凡人城镇施工已开始：先清理占地上方，再分批建造。原点："
                + origin.toShortString() + "。用 /xiuxian town status 查看进度。"));
        return true;
    }

    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) MortalTownConstruction.tick(level);
    }

    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) MortalTownConstruction.release(level);
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiuxian")
                .then(Commands.literal("town")
                        .then(Commands.literal("generate").requires(source -> source.hasPermission(2))
                                .executes(context -> generateCommand(context.getSource(), null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> generateCommand(context.getSource(),
                                                BlockPosArgument.getBlockPos(context, "pos")))))
                        .then(Commands.literal("status").executes(context -> {
                            var source = context.getSource();
                            source.sendSuccess(() -> Component.literal(MortalTownConstruction.status(
                                    source.getServer().overworld())), false);
                            return 1;
                        }))
                        .then(Commands.literal("rebuild").requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    var level = context.getSource().getServer().overworld();
                                    var data = MortalTownConstruction.data(level);
                                    if (data.origin != null || data.origins.isEmpty()) return 0;
                                    long last = data.origins.get(data.origins.size() - 1);
                                    data.origins.remove(data.origins.size() - 1);
                                    boolean queued = false;
                                    try { queued = generate(context.getSource().getPlayerOrException(), BlockPos.of(last)); }
                                    finally { if (!queued) data.origins.add(last); data.setDirty(); }
                                    return queued ? 1 : 0;
                                }))
                        .then(Commands.literal("cancel").requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    MortalTownConstruction.cancel(context.getSource().getServer().overworld());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "凡人城镇施工已取消，已施工的部分保留。"), false);
                                    return 1;
                                }))));
    }

    private static int generateCommand(CommandSourceStack source, BlockPos explicitOrigin) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return generate(player, explicitOrigin == null ? player.blockPosition().below() : explicitOrigin) ? 1 : 0;
        } catch (CommandSyntaxException failure) {
            source.sendFailure(Component.literal("此生成命令只能由玩家执行。"));
            return 0;
        }
    }
}
