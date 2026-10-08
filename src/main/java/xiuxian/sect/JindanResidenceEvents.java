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

/** Commands, tick processing and no-natural-monster protection for the inspection palace. */
@Mod.EventBusSubscriber(modid = "xiuxian")
public final class JindanResidenceEvents {
    private JindanResidenceEvents() {}

    public static boolean generate(ServerPlayer player, BlockPos origin) {
        player.sendSystemMessage(Component.literal("正在勘定金丹居所地基，请稍候……"));
        if (!player.isCreative() || !player.hasPermissions(2)) { player.sendSystemMessage(Component.literal("Jin-Dan residence inspection requires creative operator permission.")); return false; }
        if (player.level().dimension() != Level.OVERWORLD) { player.sendSystemMessage(Component.literal("The standalone Jin-Dan residence can only be built in the Overworld.")); return false; }
        int highest = player.serverLevel().getMaxBuildHeight() - 1 - JindanResidenceGenerator.MAX_Y;
        if ((long) origin.getY() + JindanResidenceGenerator.MIN_Y < player.serverLevel().getMinBuildHeight()
                || (long) origin.getY() + JindanResidenceGenerator.MAX_Y >= player.serverLevel().getMaxBuildHeight()) {
            player.sendSystemMessage(Component.literal("高度不合法：金丹居所需要基准 Y 在 "
                    + (player.serverLevel().getMinBuildHeight() - JindanResidenceGenerator.MIN_Y) + " 到 " + highest + " 之间，当前为 " + origin.getY() + "。"));
            return false;
        }
        var data = JindanResidenceConstruction.active(player.serverLevel());
        if (data.origin != null) {
            if (data.problem != null && !data.problem.isEmpty() && data.origin.equals(origin)) {
                JindanResidenceConstruction.cancel(player.serverLevel());
                player.sendSystemMessage(Component.literal("检测到上次施工已暂停，已清除暂停状态，正在按新蓝图重新开始。"));
            } else {
                player.sendSystemMessage(Component.literal("此处已有金丹居所施工记录：" + data.origin.toShortString()
                        + (data.problem == null || data.problem.isEmpty() ? "（正在施工）" : "（已暂停：" + data.problem + "）")
                        + "。请先使用 /xiuxian jindan cancel，或在原位置再次使用验收道具重试。"));
                return false;
            }
        }
        if (data.contains(origin)) {
            player.sendSystemMessage(Component.literal("此处的金丹居所已经完成，不能重复生成。"));
            return false;
        }
        try {
            for (int x : new int[] {JindanResidenceGenerator.MIN_X, JindanResidenceGenerator.MAX_X}) {
                for (int z : new int[] {JindanResidenceGenerator.MIN_Z, JindanResidenceGenerator.MAX_Z}) {
                    BlockPos corner = origin.offset(x, 0, z);
                    if (!player.serverLevel().getWorldBorder().isWithinBounds(corner)) {
                        player.sendSystemMessage(Component.literal("世界边界不够大：建筑角点 " + corner.toShortString() + " 超出边界。请把玩家移到更大的世界边界内。"));
                        return false;
                    }
                }
            }
            if (!JindanResidenceGenerator.generate(player.serverLevel(), origin)) {
                player.sendSystemMessage(Component.literal("生成失败：施工记录未能创建，请执行 /xiuxian jindan status 查看具体状态。"));
                return false;
            }
        } catch (IllegalArgumentException failure) {
            player.sendSystemMessage(Component.literal("金丹居所蓝图越界，施工没有开始：" + failure.getMessage()));
            return false;
        } catch (RuntimeException failure) {
            player.sendSystemMessage(Component.literal("金丹居所蓝图加载失败，施工没有开始：" + String.valueOf(failure.getMessage())));
            return false;
        }
        player.sendSystemMessage(Component.literal("金丹居所验收工程已排队，施工将按六个阶段逐段进行：" + origin.toShortString() + "。使用 /xiuxian jindan status 查看进度。")); return true;
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && (level.dimension() == Level.OVERWORLD || level.dimension() == LuoxiaInnerDimension.LEVEL))
            JindanResidenceConstruction.tick(level);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { event.getServer().getAllLevels().forEach(JindanResidenceConstruction::release); }
    @SubscribeEvent public static void naturalMonsterSpawn(MobSpawnEvent.PositionCheck event) {
        if(event.getSpawnType()!=MobSpawnType.NATURAL||event.getEntity().getType().getCategory()!=MobCategory.MONSTER)return;
        ServerLevel l=event.getLevel().getLevel();if(l.dimension()!=Level.OVERWORLD)return;var d=JindanResidenceConstruction.data(l);if(d.origin!=null&&inside(d.origin,event.getEntity().blockPosition())){event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);return;}for(long o:d.origins)if(inside(BlockPos.of(o),event.getEntity().blockPosition())){event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);return;}
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiuxian").then(Commands.literal("jindan")
            .then(Commands.literal("inner")
                .then(Commands.literal("status").executes(c -> {
                    var level = c.getSource().getServer().getLevel(LuoxiaInnerDimension.LEVEL);
                    if (level == null) { c.getSource().sendFailure(Component.literal("落霞洞天尚未加载。")); return 0; }
                    LuoxiaInnerRealmGenerator.ensureGenerated(level);
                    c.getSource().sendSuccess(() -> Component.literal(JindanResidenceConstruction.status(level)), false);
                    return 1;
                }))
                .then(Commands.literal("visit").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("entrance").executes(c -> visitInner(c.getSource(), "entrance")))
                    .then(Commands.literal("palace").executes(c -> visitInner(c.getSource(), "palace")))
                    .then(Commands.literal("furnace").executes(c -> visitInner(c.getSource(), "furnace")))
                    .then(Commands.literal("view").executes(c -> visitInner(c.getSource(), "view")))))
            .then(Commands.literal("generate").requires(s->s.hasPermission(2)).executes(c->generateCommand(c.getSource(),null)).then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->generateCommand(c.getSource(),BlockPosArgument.getBlockPos(c,"pos")))))
            .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal(JindanResidenceConstruction.status(c.getSource().getServer().overworld())),false);return 1;}))
            .then(Commands.literal("rebuild").requires(s -> s.hasPermission(2)).executes(c -> {
                var data = JindanResidenceConstruction.data(c.getSource().getServer().overworld());
                if (data.origin != null || data.origins.isEmpty()) return 0;
                long last = data.origins.remove(data.origins.size() - 1);
                boolean queued = false;
                try { queued = generate(c.getSource().getPlayerOrException(), BlockPos.of(last)); }
                finally { if (!queued) data.origins.add(last); data.setDirty(); }
                return queued ? 1 : 0;
            }))
            .then(Commands.literal("cancel").requires(s->s.hasPermission(2)).executes(c->{JindanResidenceConstruction.cancel(c.getSource().getServer().overworld());c.getSource().sendSuccess(()->Component.literal("Jin-Dan construction cancelled; written blocks remain for inspection."),false);return 1;}))));
    }
    private static int generateCommand(CommandSourceStack source,BlockPos explicit){try{ServerPlayer p=source.getPlayerOrException();return generate(p,explicit==null?p.blockPosition().below():explicit)?1:0;}catch(CommandSyntaxException e){source.sendFailure(Component.literal("Run Jin-Dan generation as a player."));return 0;}}

    private static int visitInner(CommandSourceStack source, String point) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isCreative()) {
            source.sendFailure(Component.literal("金丹居所验收传送需要创造模式。"));
            return 0;
        }
        var level = source.getServer().getLevel(LuoxiaInnerDimension.LEVEL);
        if (level == null) { source.sendFailure(Component.literal("落霞洞天尚未加载。")); return 0; }
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        if (!LuoxiaJindanResidence.isReady(level)) {
            source.sendFailure(Component.literal("金丹居所尚在施工，请用 /xiuxian jindan inner status 查看进度。"));
            return 0;
        }
        if (!LuoxiaInnerDimension.isInner(player.level())) {
            var id = player.getUUID();
            if (!LuoxiaInnerDimension.enter(player)) return 0;
            player = source.getServer().getPlayerList().getPlayer(id);
            if (player == null) return 0;
        }
        BlockPos target = switch (point) {
            case "palace" -> LuoxiaJindanResidence.ORIGIN.offset(92, 10, 0);
            case "furnace" -> LuoxiaJindanResidence.ORIGIN.offset(4, 18, 4);
            case "view" -> LuoxiaJindanResidence.ORIGIN.offset(205, 90, 205);
            default -> LuoxiaJindanResidence.ORIGIN.offset(177, 8, 0);
        };
        if (point.equals("view")) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        player.teleportTo(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 90, 0);
        player.fallDistance = 0;
        return 1;
    }
    private static boolean inside(BlockPos o,BlockPos p){return p.getX()>=o.getX()+JindanResidenceGenerator.MIN_X&&p.getX()<=o.getX()+JindanResidenceGenerator.MAX_X&&p.getY()>=o.getY()+JindanResidenceGenerator.MIN_Y&&p.getY()<=o.getY()+JindanResidenceGenerator.MAX_Y&&p.getZ()>=o.getZ()+JindanResidenceGenerator.MIN_Z&&p.getZ()<=o.getZ()+JindanResidenceGenerator.MAX_Z;}
}
