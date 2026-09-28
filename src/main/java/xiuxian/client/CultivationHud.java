package xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivationHud {
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 63;
    private static final int GOLD = 0xFFD4B46A;
    private static final int BAR_BACK = 0xFF161916;

    private CultivationHud() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !CultivationClientState.isInitialized()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 8;
        int y = 8;
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xB70D1411);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 1, GOLD);
        graphics.fill(x, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF78633D);
        graphics.fill(x, y, x + 1, y + PANEL_HEIGHT, 0xFF78633D);
        graphics.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF78633D);

        String realm = CultivationClientState.realm().displayName() + " · 第 "
                + CultivationClientState.realmLevel() + " 层";
        graphics.drawString(minecraft.font, realm, x + 8, y + 6, GOLD, false);
        graphics.fill(x + 8, y + 18, x + PANEL_WIDTH - 8, y + 19, 0x885E725F);

        float health = minecraft.player.getHealth();
        float maxHealth = minecraft.player.getMaxHealth();
        int roundedHealth = Math.round(health);
        int roundedMaxHealth = Math.round(maxHealth);
        drawMeter(graphics, x + 8, y + 22, PANEL_WIDTH - 16,
                "气血", roundedHealth + "/" + roundedMaxHealth,
                maxHealth <= 0 ? 0 : health / maxHealth, 0xFFB85D58);

        int qi = CultivationClientState.qi();
        int cost = CultivationClientState.breakthroughCost();
        drawMeter(graphics, x + 8, y + 42, PANEL_WIDTH - 16,
                "修为", qi + "/" + cost,
                cost <= 0 ? 0 : qi / (float) cost, 0xFF70A98C);
    }

    private static void drawMeter(GuiGraphics graphics, int x, int y, int width,
                                  String label, String value, float progress, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(minecraft.font, label, x, y, 0xFFE4DDC9, false);
        graphics.drawString(minecraft.font, value, x + width - minecraft.font.width(value), y,
                0xFFD2CBB9, false);

        int barY = y + 10;
        graphics.fill(x, barY, x + width, barY + 4, BAR_BACK);
        graphics.fill(x, barY, x + width, barY + 1, 0xFF66583C);
        int fillWidth = Math.round((width - 2) * Math.max(0, Math.min(1, progress)));
        if (fillWidth > 0) {
            graphics.fill(x + 1, barY + 1, x + 1 + fillWidth, barY + 3, color);
        }
    }
}
