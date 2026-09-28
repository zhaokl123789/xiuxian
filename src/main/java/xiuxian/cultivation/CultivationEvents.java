package xiuxian.cultivation;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xiuxian.item.XiuxianItems;

public class CultivationEvents {
    @SubscribeEvent
    public void attachPlayerData(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            CultivationCapability.Provider provider = new CultivationCapability.Provider();
            event.addCapability(CultivationCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(CultivationCapability.CULTIVATION).ifPresent(original ->
                event.getEntity().getCapability(CultivationCapability.CULTIVATION)
                        .ifPresent(copy -> copy.copyFrom(original)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CultivationData data = getData(player);
        if (data != null && !data.isInitialized()) {
            showIdentityChoices(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        CultivationData data = getData(player);
        if (data != null && data.tickMeditation(player.getX(), player.getY(), player.getZ())
                && data.qi() % 10 == 0) {
            player.sendSystemMessage(Component.literal("打坐凝神，当前修为：" + data.qi()));
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiuxian")
                .then(Commands.literal("start")
                        .then(Commands.argument("family", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(FamilyOrigin.ids(), builder))
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(CultivationPath.ids(), builder))
                                        .executes(context -> start(context.getSource(),
                                                StringArgumentType.getString(context, "family"),
                                                StringArgumentType.getString(context, "path"))))))
                .then(Commands.literal("identity").executes(context -> identity(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("meditate").executes(context -> toggleMeditation(context.getSource())))
                .then(Commands.literal("breakthrough").executes(context -> breakthrough(context.getSource())))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "命令：/xiuxian identity、/xiuxian start <family> <path>、/xiuxian status、/xiuxian meditate、/xiuxian breakthrough"), false);
                    return 1;
                }));
    }

    private static int start(CommandSourceStack source, String familyId, String pathId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        FamilyOrigin family = FamilyOrigin.byId(familyId);
        CultivationPath path = CultivationPath.byId(pathId);
        if (family == null || path == null) {
            source.sendFailure(Component.literal("身份选项无效，请从聊天提示中选择，或使用 Tab 查看选项。"));
            return 0;
        }

        CultivationData data = getData(player);
        if (data == null) {
            source.sendFailure(Component.literal("无法读取修行数据。"));
            return 0;
        }
        if (data.isInitialized()) {
            source.sendFailure(Component.literal("你的修行身份已经确立，暂不支持重新选择。"));
            return 0;
        }

        data.begin(family, path);
        player.addItem(new ItemStack(XiuxianItems.QI_GATHERING_PILL.get()));
        source.sendSuccess(() -> Component.literal("你以人类之身踏入修行路，出身：" + family.displayName()
                + "，身份：" + path.displayName() + "。你已领悟入门功法：吐纳引气诀，并获得一枚凝气丹。"), false);
        source.sendSuccess(() -> Component.literal("输入 /xiuxian meditate 开始打坐，/xiuxian status 查看修为，/xiuxian breakthrough 尝试突破。"), false);
        return 1;
    }

    private static int identity(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null) {
            source.sendFailure(Component.literal("无法读取修行数据。"));
            return 0;
        }
        if (data.isInitialized()) {
            source.sendFailure(Component.literal("你的修行身份已经确立，暂不支持重新选择。"));
            return 0;
        }
        showIdentityChoices(player);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("你尚未选择修行身份。使用 /xiuxian identity 查看选项。"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("种族：人类 | 出身：" + data.familyOrigin().displayName()
                + " | 修行身份：" + data.cultivationPath().displayName()), false);
        source.sendSuccess(() -> Component.literal("境界：" + data.realm().displayName() + data.realmLevel()
                + "层 | 修为：" + data.qi() + "/" + data.breakthroughCost()), false);
        source.sendSuccess(() -> Component.literal("功法：吐纳引气诀 | 状态："
                + (data.isMeditating() ? "打坐中" : "未打坐")), false);
        return 1;
    }

    private static int toggleMeditation(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("先选择身份：使用 /xiuxian identity 并点击一项出身。"));
            return 0;
        }

        if (data.isMeditating()) {
            data.stopMeditating();
            source.sendSuccess(() -> Component.literal("你结束了打坐，积累修为：" + data.qi()), false);
        } else {
            data.startMeditating(player.getX(), player.getY(), player.getZ());
            source.sendSuccess(() -> Component.literal("你开始运转吐纳引气诀。保持静止，每秒积累 1 点修为。"), false);
        }
        return 1;
    }

    private static int breakthrough(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("先选择身份：使用 /xiuxian identity 并点击一项出身。"));
            return 0;
        }

        data.stopMeditating();
        if (data.qi() < data.breakthroughCost()) {
            source.sendFailure(Component.literal("修为不足，需要 " + data.breakthroughCost() + " 点，目前有 " + data.qi() + " 点。"));
            return 0;
        }
        if (!data.breakthrough()) {
            source.sendFailure(Component.literal("你已达到当前版本的境界上限。"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("突破成功！当前境界：" + data.realm().displayName()
                + data.realmLevel() + "层。"), false);
        return 1;
    }

    private static CultivationData getData(Player player) {
        return player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
    }

    private static void showIdentityChoices(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("初入修行界，请选择家族出身与修行身份：").withStyle(ChatFormatting.GOLD));
        for (FamilyOrigin family : FamilyOrigin.values()) {
            for (CultivationPath path : CultivationPath.values()) {
                String command = "/xiuxian start " + family.id() + " " + path.id();
                Component choice = Component.literal("[" + family.displayName() + " · " + path.displayName() + "]")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("选择此身份并获得入门功法"))));
                player.sendSystemMessage(choice);
            }
        }
    }
}
