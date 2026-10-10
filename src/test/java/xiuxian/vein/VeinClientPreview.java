package xiuxian.vein;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.block.VeinBlocks;
import xiuxian.client.screen.VeinStationScreen;

/** Opt-in native client rendering check, without entering or modifying a saved world. */
@Mod.EventBusSubscriber(modid="xiuxian", value=Dist.CLIENT)
public final class VeinClientPreview {
    private static int phase, ticks, originalScale;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("xiuxian.veinPreview") || event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.getOverlay()!=null) return;
        if(phase==0 && mc.screen instanceof TitleScreen) {
            originalScale=mc.options.guiScale().get();
            mc.options.guiScale().set(1);mc.resizeDisplay();mc.setScreen(new Gallery());phase=1;ticks=0;
        } else if(phase==1 && ++ticks==30) {
            Screenshot.grab(mc.gameDirectory,"vein-client-gallery.png",mc.getMainRenderTarget(),message -> {});
            mc.options.guiScale().set(2);mc.resizeDisplay();
            mc.setScreen(new Screen(Component.literal("Vein station preview")) {
                private final VeinStationScreen preview=new VeinStationScreen(new VeinStationMenu(1,new Inventory(null)),new Inventory(null),Component.translatable("block.xiuxian.vein_condenser"));
                @Override protected void init() { preview.init(minecraft,width,height); }
                @Override public void render(GuiGraphics g,int x,int y,float tick) { preview.render(g,x,y,tick); }
            });
            phase=2;ticks=0;
        } else if(phase==2 && ++ticks==30) {
            Screenshot.grab(mc.gameDirectory,"vein-client-station.png",mc.getMainRenderTarget(),message -> {});phase=3;ticks=0;
        } else if(phase==3 && ++ticks==30) { mc.options.guiScale().set(originalScale);mc.stop(); }
    }
    private static final class Gallery extends Screen {
        private final java.util.List<java.util.Map.Entry<String, net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.Block>>> blocks=new ArrayList<>(VeinBlocks.resources().entrySet());
        Gallery() { super(Component.literal("Vein resources"));blocks.sort(java.util.Map.Entry.comparingByKey()); }
        @Override public void render(GuiGraphics g,int mx,int my,float tick) {
            g.fill(0,0,width,height,0xFF20282B);g.drawString(font,"Nine-layer vein | 81 dedicated blocks",12,8,0xFFE2ECEB,false);
            int cw=(width-24)/9,ch=(height-32)/9;
            for(int i=0;i<blocks.size();i++) {
                int x=12+(i%9)*cw,y=28+(i/9)*ch;
                g.pose().pushPose();g.pose().translate(x+cw/2-16,y,0);g.pose().scale(2,2,1);
                g.renderItem(new ItemStack(blocks.get(i).getValue().get()),0,0);g.pose().popPose();
                String label=blocks.get(i).getKey().substring(5);
                g.drawString(font,font.plainSubstrByWidth(label,cw-2),x,y+34,0xFFBED0CC,false);
            }
        }
    }
}
