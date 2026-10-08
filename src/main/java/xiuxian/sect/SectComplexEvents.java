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
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="xiuxian")
public final class SectComplexEvents {
    private SectComplexEvents() {}

    public static boolean generate(ServerPlayer player,BlockPos origin) {
        if(!player.isCreative()||!player.hasPermissions(2))return fail(player,"宗门验收建造需要创造模式和管理员权限。");
        var level=player.serverLevel();
        if(level.dimension()!=Level.OVERWORLD)return fail(player,"独立验收建造请在主世界使用；洞天宗门会自动移植，可用 /xiuxian sect inner status 查看进度。");
        if(!SectComplexGenerator.validSite(level,origin))return fail(player,"场地越界或高度不合法；主世界基准 Y 应在 -40 到 165 之间。");
        if(!SectComplexGenerator.generate(level,origin))return fail(player,"此位置已完成建造，或已有施工任务。使用 /xiuxian sect status 查看。");
        player.getAbilities().flying=true;player.onUpdateAbilities();
        player.teleportTo(level,origin.getX()+218.5,origin.getY()+145,origin.getZ()+248.5,140,24);
        player.fallDistance=0;
        player.sendSystemMessage(Component.literal("完整宗门已排队：449×513。场地内从基准 Y-24 到世界顶端全部清空，随后复查并分区建造。"));
        return true;
    }
    private static boolean fail(ServerPlayer player,String message){player.sendSystemMessage(Component.literal(message));return false;}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level
                &&(level.dimension()==Level.OVERWORLD||level.dimension()==LuoxiaInnerDimension.LEVEL))
            SectComplexConstruction.tick(level);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){event.getServer().getAllLevels().forEach(SectComplexConstruction::release);}
    static boolean protects(ServerLevel level,BlockPos pos) {
        if(level.dimension()!=Level.OVERWORLD&&level.dimension()!=LuoxiaInnerDimension.LEVEL)return false;
        var d=SectComplexConstruction.data(level);
        if(level.dimension()==LuoxiaInnerDimension.LEVEL&&(d.origin!=null||d.contains(LuoxiaSectComplexResidence.ORIGIN))
                &&LuoxiaSectComplexResidence.onApproach(pos))return true;
        if(d.origin!=null&&inside(level,d.origin,pos))return true;
        return d.origins.stream().anyMatch(value->inside(level,BlockPos.of(value),pos));
    }
    private static boolean inside(ServerLevel level,BlockPos origin,BlockPos pos) {
        return pos.getX()>=origin.getX()+SectComplexGenerator.MIN_X&&pos.getX()<=origin.getX()+SectComplexGenerator.MAX_X
                &&pos.getZ()>=origin.getZ()+SectComplexGenerator.MIN_Z&&pos.getZ()<=origin.getZ()+SectComplexGenerator.MAX_Z
                &&pos.getY()>=origin.getY()+SectComplexGenerator.MIN_Y&&pos.getY()<level.getMaxBuildHeight();
    }
    @SubscribeEvent public static void naturalMonsterSpawn(MobSpawnEvent.PositionCheck event) {
        if(event.getSpawnType()==MobSpawnType.NATURAL&&event.getEntity().getType().getCategory()==MobCategory.MONSTER
                &&protects(event.getLevel().getLevel(),event.getEntity().blockPosition()))event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        var root=Commands.literal("sect")
                .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal(SectComplexConstruction.status(c.getSource().getServer().overworld())),false);return 1;}))
                .then(Commands.literal("generate").requires(s->s.hasPermission(2))
                        .executes(c->generateCommand(c.getSource(),null))
                        .then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->generateCommand(c.getSource(),BlockPosArgument.getBlockPos(c,"pos")))))
                .then(Commands.literal("cancel").requires(s->s.hasPermission(2)).executes(c->{
                    SectComplexConstruction.cancel(c.getSource().getServer().overworld());
                    c.getSource().sendSuccess(()->Component.literal("宗门施工已取消，已写入的方块保留。"),false);return 1;}))
                .then(Commands.literal("rebuild").requires(s->s.hasPermission(2)).executes(c->rebuild(c.getSource())))
                .then(Commands.literal("retry").requires(s->s.hasPermission(2)).executes(c->retry(c.getSource())));
        var visit=Commands.literal("visit").requires(s->s.hasPermission(2));
        for(String point:new String[]{"entrance","main","library","music","garden","water","view"})
            visit.then(Commands.literal(point).executes(c->visit(c.getSource(),point)));
        root.then(visit);
        var inner=Commands.literal("inner").then(Commands.literal("retry").requires(s->s.hasPermission(2)).executes(c->{
            var player=c.getSource().getPlayerOrException();
            var level=c.getSource().getServer().getLevel(LuoxiaInnerDimension.LEVEL);
            return player.isCreative()&&level!=null&&SectComplexConstruction.retryInner(level)?1:0;
        })).then(Commands.literal("status").executes(c->{
            var level=c.getSource().getServer().getLevel(LuoxiaInnerDimension.LEVEL);
            if(level==null)return 0;
            LuoxiaInnerRealmGenerator.ensureGenerated(level);
            c.getSource().sendSuccess(()->Component.literal(SectComplexConstruction.status(level)),false);return 1;
        }));
        var innerVisit=Commands.literal("visit").requires(s->s.hasPermission(2));
        for(String point:new String[]{"entrance","main","library","music","garden","water","view"})
            innerVisit.then(Commands.literal(point).executes(c->visitInner(c.getSource(),point)));
        inner.then(innerVisit);root.then(inner);
        event.getDispatcher().register(Commands.literal("xiuxian").then(root));
    }
    private static int generateCommand(CommandSourceStack source,BlockPos explicit) throws CommandSyntaxException {
        var player=source.getPlayerOrException();return generate(player,explicit==null?player.blockPosition().below():explicit)?1:0;
    }
    private static int visitInner(CommandSourceStack source,String point) throws CommandSyntaxException {
        var player=source.getPlayerOrException();
        if(!player.isCreative())return 0;
        var level=source.getServer().getLevel(LuoxiaInnerDimension.LEVEL);
        if(level==null)return 0;
        LuoxiaInnerRealmGenerator.ensureGenerated(level);
        if(!LuoxiaSectComplexResidence.isReady(level)) {
            source.sendFailure(Component.literal("宗门仍在施工，请用 /xiuxian sect inner status 查看进度。"));return 0;
        }
        if(!LuoxiaInnerDimension.isInner(player.level())) {
            var id=player.getUUID();if(!LuoxiaInnerDimension.enter(player))return 0;
            player=source.getServer().getPlayerList().getPlayer(id);if(player==null)return 0;
        }
        var target=LuoxiaSectComplexResidence.ORIGIN.offset(SectComplexGenerator.createPlan().visits.get(point));
        if(point.equals("view")){player.getAbilities().flying=true;player.onUpdateAbilities();}
        player.teleportTo(level,target.getX()+0.5,target.getY(),target.getZ()+0.5,180,point.equals("view")?24:0);
        player.fallDistance=0;return 1;
    }
    private static int rebuild(CommandSourceStack source) throws CommandSyntaxException {
        var level=source.getServer().overworld();var d=SectComplexConstruction.data(level);
        if(d.origin!=null||d.origins.isEmpty())return 0;
        long last=d.origins.remove(d.origins.size()-1);boolean queued=false;
        try{queued=generate(source.getPlayerOrException(),BlockPos.of(last));}
        finally{if(!queued)d.origins.add(last);d.setDirty();}
        return queued?1:0;
    }
    private static int retry(CommandSourceStack source) throws CommandSyntaxException {
        var player=source.getPlayerOrException();var d=SectComplexConstruction.data(source.getServer().overworld());
        if(!player.isCreative()||player.level().dimension()!=Level.OVERWORLD||d.origin==null||d.problem.isEmpty()
                ||!SectComplexGenerator.validSite(player.serverLevel(),d.origin))return 0;
        var origin=d.origin;SectComplexConstruction.cancel(player.serverLevel());return generate(player,origin)?1:0;
    }
    private static int visit(CommandSourceStack source,String point) throws CommandSyntaxException {
        var player=source.getPlayerOrException();var level=source.getServer().overworld();
        if(!player.isCreative())return fail(player,"验收传送需要创造模式。")?1:0;
        var origin=SectComplexConstruction.latest(level);
        if(origin==null||SectComplexConstruction.data(level).origin!=null)return fail(player,"宗门尚未完工。")?1:0;
        var relative=SectComplexGenerator.createPlan().visits.get(point);var target=origin.offset(relative);
        if(point.equals("view")){player.getAbilities().flying=true;player.onUpdateAbilities();}
        player.teleportTo(level,target.getX()+0.5,target.getY(),target.getZ()+0.5,180,point.equals("view")?24:0);
        player.fallDistance=0;return 1;
    }
}
