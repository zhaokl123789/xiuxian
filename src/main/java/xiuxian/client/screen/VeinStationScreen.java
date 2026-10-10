package xiuxian.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import xiuxian.vein.VeinStationMenu;

public final class VeinStationScreen extends AbstractContainerScreen<VeinStationMenu> {
    private static final String[] SLOTS = {"input", "reagent", "fuel", "output", "residue"};
    public VeinStationScreen(VeinStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth=176; imageHeight=158;
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mouseX, int mouseY) {
        g.fill(leftPos,topPos,leftPos+176,topPos+158,0xFF252A2C);
        g.fill(leftPos,topPos,leftPos+176,topPos+2,0xFF6FBEA7);
        for (var slot:menu.slots) {
            g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xFF758183);
            g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xFF131719);
        }
        g.fill(leftPos+16,topPos+53,leftPos+160,topPos+57,0xFF121719);
        g.fill(leftPos+16,topPos+53,leftPos+16+144*menu.percent()/100,topPos+57,0xFF6FBEA7);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font,title,8,7,0xFFE2ECEB,false);
        for(int i=0;i<5;i++) {
            Component label=Component.translatable("vein.slot."+SLOTS[i]);
            g.drawString(font,label,24+32*i-font.width(label)/2,20,0xFFCAD2D3,false);
        }
        Component status=Component.translatable(menu.paused()?"vein.station.paused":"vein.station.progress",menu.percent(),menu.fuel());
        g.drawString(font,status,8,61,0xFFA6C3BA,false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float tick) { renderBackground(g); super.render(g,x,y,tick); renderTooltip(g,x,y); }
}
