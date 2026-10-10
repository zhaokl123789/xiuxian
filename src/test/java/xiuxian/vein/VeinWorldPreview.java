package xiuxian.vein;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.cultivation.*;
import xiuxian.sect.LuoxiaInnerDimension;

/** Automated screenshots inside a copied test world; never opens a user save. */
@Mod.EventBusSubscriber(modid="xiuxian",value=Dist.CLIENT)
public final class VeinWorldPreview {
    private static int phase,ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("xiuxian.veinWorldPreview") || event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();if(mc.getOverlay()!=null)return;
        if(phase==0 && mc.screen instanceof TitleScreen) {
            mc.options.pauseOnLostFocus=false;
            mc.options.hideGui=true;
            mc.createWorldOpenFlows().loadLevel(mc.screen,"vein-inspection");phase=1;
        } else if(phase==1 && mc.player!=null && mc.getSingleplayerServer()!=null) {
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(() -> {
                var player=server.getPlayerList().getPlayer(uuid);if(player==null)return;
                CultivationEvents.selectIdentity(player,FamilyOrigin.MORTAL.id(),CultivationPath.WANDERER.id());
                var level=server.getLevel(LuoxiaInnerDimension.LEVEL);
                player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,251.5,30,400.5,0,6);
            });
            phase=2;ticks=0;
        } else if(phase==2 && ++ticks==160) {
            mc.setScreen(null);phase=3;ticks=0;
        } else if(phase==3 && ++ticks==60) {
            Screenshot.grab(mc.gameDirectory,"vein-boss-room.png",mc.getMainRenderTarget(),message -> {});
            var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
            server.execute(() -> {
                var player=server.getPlayerList().getPlayer(uuid);if(player==null)return;
                var level=server.getLevel(LuoxiaInnerDimension.LEVEL);var terrain=new VeinTerrain(VeinChunkGenerator.terrainSeed(level.getChunkSource().randomState()));
                BlockPos chosen=new BlockPos(64,0,228);
                int best=0;
                for(int x=40;x<112;x+=4)for(int z=200;z<280;z+=4) {
                    int air=0;for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++)if(terrain.state(x+dx,2,z+dz).isAir())air++;
                    if(air>best && terrain.state(x,1,z).isAir()) { best=air;chosen=new BlockPos(x,0,z); }
                }
                player.teleportTo(level,chosen.getX()+.5,chosen.getY(),chosen.getZ()+.5,30,4);
            });phase=4;ticks=0;
        } else if(phase==4 && ++ticks==160) {
            Screenshot.grab(mc.gameDirectory,"vein-resource-cavern.png",mc.getMainRenderTarget(),message -> {});
            phase=5;ticks=0;
        } else if(phase==5 && ++ticks==40) mc.stop();
        if(phase>=2 && phase<=4 && mc.screen!=null) mc.setScreen(null);
    }
}
