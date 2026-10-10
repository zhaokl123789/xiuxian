package xiuxian.vein;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.sect.*;

@Mod.EventBusSubscriber(modid="xiuxian")
public final class VeinCommands {
    private VeinCommands() {}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiuxian").then(Commands.literal("vein")
            .then(Commands.literal("locate").executes(c -> {
                var p=c.getSource().getPlayerOrException();
                if(!LuoxiaInnerDimension.isInner(p.level())) { c.getSource().sendFailure(Component.literal("请先进入落霞洞天。"));return 0; }
                int layer=VeinLayers.atY(p.getBlockY());if(layer==0) layer=1;
                BlockPos hub=VeinTerrain.hub(p.getBlockX(),p.getBlockZ(),layer);
                int bossLayer=layer<=3?3:layer<=6?6:9;
                BlockPos room=VeinTerrain.room(p.getBlockX(),p.getBlockZ(),bossLayer);
                c.getSource().sendSuccess(() -> Component.literal("灵脉入口：0, 73, 160 | 邻近矿站："+hub.toShortString()+" | 分区试炼房："+room.toShortString()),false);return 1;
            }))
            .then(Commands.literal("visit").requires(s -> s.hasPermission(2))
                .then(Commands.argument("layer",IntegerArgumentType.integer(1,9)).executes(c -> {
                    var player=c.getSource().getPlayerOrException();var level=c.getSource().getServer().getLevel(LuoxiaInnerDimension.LEVEL);if(level==null)return 0;
                    int layer=IntegerArgumentType.getInteger(c,"layer");
                    BlockPos pos=LuoxiaInnerRealmGenerator.marker(level,"vein_l"+layer);
                    if(pos==null) return 0;
                    player.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,180,0);return 1;
                })))
            .then(Commands.literal("boss").requires(s -> s.hasPermission(2))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(c -> {
                    var player=c.getSource().getPlayerOrException();var level=c.getSource().getServer().getLevel(LuoxiaInnerDimension.LEVEL);if(level==null)return 0;
                    int layer=IntegerArgumentType.getInteger(c,"tier")*3;
                    BlockPos room=VeinTerrain.room(player.getBlockX(),player.getBlockZ(),layer);
                    BlockPos pos=VeinTerrain.control(room).south(2);
                    level.getChunkAt(pos);
                    if(!(level.getBlockState(VeinTerrain.control(room)).getBlock() instanceof VeinSwitchBlock)
                            || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                        c.getSource().sendFailure(Component.literal("这里是旧版地下区域，尚未生成新试炼房。请到新探索的区域查找。"));return 0;
                    }
                    player.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);return 1;
                })))
        ));
    }
}
