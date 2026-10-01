package xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import xiuxian.cultivation.CultivationTechnique;
import xiuxian.cultivation.CultivationTechniques;
import xiuxian.client.screen.CultivationProfileScreen;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivationHud {
    private static final int MAX_PANEL_WIDTH = 112;
    private static final int PANEL_HEIGHT = 50;
    private static final int GOLD = 0xFFD4B46A;
    private static final int BAR_BACK = 0xFF161916;

    private CultivationHud() {}

    @SubscribeEvent
    public static void hideExtendedVanillaHealth(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && CultivationClientState.isInitialized()
                && layout(minecraft) != null
                && event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !CultivationClientState.isInitialized()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        HudLayout layout = layout(minecraft);
        if (layout == null) {
            return;
        }
        float health = minecraft.player.getHealth();
        float maxHealth = minecraft.player.getMaxHealth();
        int roundedHealth = Math.round(health);
        int roundedMaxHealth = Math.round(maxHealth);
        int qi = CultivationClientState.qi();
        int cost = CultivationClientState.breakthroughCost();

        drawMeter(graphics, layout.leftX(), layout.y(), layout.panelWidth(), "气血",
                roundedHealth + "/" + roundedMaxHealth,
                maxHealth <= 0 ? 0 : health / maxHealth, 0xFFB85D58, false);
        drawMeter(graphics, layout.rightX(), layout.y(), layout.panelWidth(), "修为", qi + "/" + cost,
                cost <= 0 ? 0 : qi / (float) cost, 0xFF70A98C, true);
        drawTrueQi(graphics, layout.leftX(), layout.y(), layout.panelWidth());
        drawTechniqueStudy(graphics, layout.screenWidth());

        int mouseX = guiMouseX(minecraft);
        int mouseY = guiMouseY(minecraft);
        if (isArchiveButtonHovered(layout, mouseX, mouseY)) {
            graphics.renderTooltip(minecraft.font, Component.literal("打开修行档案"), mouseX, mouseY);
        }
    }

    @SubscribeEvent
    public static void openArchiveFromHud(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_1 || event.getAction() != GLFW.GLFW_PRESS
                || minecraft.player == null || !CultivationClientState.isInitialized()
                || (minecraft.screen != null && !(minecraft.screen instanceof InventoryScreen))) {
            return;
        }

        HudLayout layout = layout(minecraft);
        if (layout != null && isArchiveButtonHovered(layout, guiMouseX(minecraft), guiMouseY(minecraft))) {
            minecraft.setScreen(new CultivationProfileScreen(minecraft.screen));
            event.setCanceled(true);
        }
    }

    private static void drawTechniqueStudy(GuiGraphics graphics, int screenWidth) {
        String id = CultivationClientState.studyingTechniqueId();
        if (id.isEmpty()) {
            return;
        }
        CultivationTechnique technique = CultivationTechniques.byId(id);
        if (technique == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.min(240, screenWidth - 16);
        int height = 48;
        int x = (screenWidth - width) / 2;
        int y = 42;
        int totalTicks = CultivationClientState.techniqueStudyDuration();
        float progress = totalTicks == 0 ? 0.0F
                : Math.min(1.0F, CultivationClientState.techniqueStudyTicks() / (float) totalTicks);
        int percent = Math.round(progress * 100);
        String title = "参悟《" + technique.displayName() + "》";
        String chance = "识海映象 · 成功率 " + CultivationClientState.techniqueStudyChance() + "% · " + percent + "%";

        drawTechniqueWaterfall(graphics, technique, screenWidth, minecraft.getWindow().getGuiScaledHeight());
        graphics.fill(x, y, x + width, y + height, 0xD00D1512);
        graphics.fill(x, y, x + width, y + 1, GOLD);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF78633D);
        graphics.drawCenteredString(minecraft.font, title, screenWidth / 2, y + 5, 0xFFE7D8AF);
        graphics.drawCenteredString(minecraft.font, chance, screenWidth / 2, y + 17, 0xFFBCC9BC);

        int barX = x + 10;
        int barY = y + 33;
        int barWidth = width - 20;
        graphics.fill(barX, barY, barX + barWidth, barY + 7, BAR_BACK);
        graphics.fill(barX, barY, barX + barWidth, barY + 1, 0xFF66583C);
        int fillWidth = Math.round((barWidth - 2) * progress);
        if (fillWidth > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + 6, 0xFF9BC9AE);
        }
    }

    private static void drawTechniqueWaterfall(GuiGraphics graphics, CultivationTechnique technique,
                                               int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        String[] glyphs = {"\u6c14", "\u7ecf", "\u8109", "\u795e", "\u610f", "\u4e39", "\u5143", "\u9053", "\u606f", "\u5b9a"};
        int columns = Math.max(8, screenWidth / 34);
        for (int column = 0; column < columns; column++) {
            int x = 10 + column * 34;
            int speed = 1 + Math.floorMod(column * 7, 3);
            int offset = Math.floorMod((int) (tick * speed + column * 47), Math.max(1, screenHeight + 120));
            int length = 5 + Math.floorMod(column * 3, 6);
            for (int row = 0; row < length; row++) {
                int y = offset - row * 14;
                if (y < 6 || y > screenHeight - 8) continue;
                int alpha = Math.max(35, 210 - row * 28);
                int color = (alpha << 24) | (row == 0 ? 0xD8F2D0 : 0x6FB8A0);
                String glyph = glyphs[Math.floorMod(column + row + technique.id().hashCode(), glyphs.length)];
                graphics.drawString(minecraft.font, glyph, x, y, color, false);
            }
        }
        int pulse = 80 + Math.floorMod((int) tick, 80);
        graphics.fill(screenWidth / 2 - 1, 4, screenWidth / 2 + 1, screenHeight - 4,
                (pulse << 24) | 0x5D8D7B);
    }

    private static void drawMeter(GuiGraphics graphics, int x, int y, int width,
                                  String label, String value, float progress, int color, boolean archiveButton) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(x, y, x + width, y + PANEL_HEIGHT, 0xB70D1411);
        graphics.fill(x, y, x + width, y + 1, GOLD);
        graphics.fill(x, y + PANEL_HEIGHT - 1, x + width, y + PANEL_HEIGHT, 0xFF78633D);
        graphics.fill(x, y, x + 1, y + PANEL_HEIGHT, 0xFF78633D);
        graphics.fill(x + width - 1, y, x + width, y + PANEL_HEIGHT, 0xFF78633D);

        String shortLabel = width < 84 ? label.substring(0, 1) : label;
        if (minecraft.font.width(shortLabel) + minecraft.font.width(value) + 4 > width - 8) {
            shortLabel = "";
        }
        graphics.drawString(minecraft.font, shortLabel, x + 5, y + 4, 0xFFE4DDC9, false);
        boolean drawArchiveButton = archiveButton && width >= 76;
        int rightPadding = drawArchiveButton ? 21 : 5;
        graphics.drawString(minecraft.font, value, x + width - rightPadding - minecraft.font.width(value), y + 4,
                0xFFD2CBB9, false);

        if (drawArchiveButton) {
            int buttonX = x + width - 15;
            graphics.fill(buttonX, y + 3, buttonX + 12, y + 15, 0xFF78633D);
            graphics.fill(buttonX + 1, y + 4, buttonX + 11, y + 14, 0xFF26332C);
            graphics.drawCenteredString(minecraft.font, "?", buttonX + 6, y + 5, 0xFFE5D5AA);
        }

        int barX = x + 5;
        int barWidth = width - 10;
        int barY = y + 19;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, BAR_BACK);
        graphics.fill(barX, barY, barX + barWidth, barY + 1, 0xFF66583C);
        if (label.equals("气血")) {
            drawSegmentedHealth(graphics, barX + 1, barY + 1, barWidth - 2, 3, progress);
        } else {
            int fillWidth = Math.round((barWidth - 2) * Math.max(0, Math.min(1, progress)));
            if (fillWidth > 0) {
                graphics.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + 4, color);
            }
        }
    }

    private static void drawSegmentedHealth(GuiGraphics graphics, int x, int y, int width, int height,
                                            float progress) {
        int[] colors = {0xFFB84545, 0xFFCF584B, 0xFFD96F46, 0xFFE09A48,
                0xFFCEB24C, 0xFF9FB45A, 0xFF69A878, 0xFF4C9BA0};
        int segments = Math.max(4, Math.min(colors.length, width / 5));
        int gap = 1;
        int segmentWidth = Math.max(1, (width - (segments - 1) * gap) / segments);
        float clampedProgress = Math.max(0.0F, Math.min(1.0F, progress));
        for (int i = 0; i < segments; i++) {
            int left = x + i * (segmentWidth + gap);
            float segmentFill = Math.max(0.0F, Math.min(1.0F, clampedProgress * segments - i));
            graphics.fill(left, y, left + segmentWidth, y + height, 0xFF302622);
            int filledWidth = Math.round(segmentWidth * segmentFill);
            if (filledWidth > 0) {
                graphics.fill(left, y, left + filledWidth, y + height, colors[i]);
            }
        }
    }

    private static void drawTrueQi(GuiGraphics graphics, int x, int y, int width) {
        Minecraft minecraft = Minecraft.getInstance();
        int qi = CultivationClientState.trueQi();
        int maximum = CultivationClientState.trueQiMaximum();
        String value = qi + "/" + maximum;
        graphics.drawString(minecraft.font, "真炁", x + 5, y + 26, 0xFFE4DDC9, false);
        graphics.drawString(minecraft.font, value, x + width - 5 - minecraft.font.width(value), y + 26,
                0xFFD2CBB9, false);
        int barX = x + 5;
        int barY = y + 39;
        int barWidth = width - 10;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, BAR_BACK);
        graphics.fill(barX, barY, barX + barWidth, barY + 1, 0xFF66583C);
        int fillWidth = maximum <= 0 ? 0 : Math.round((barWidth - 2)
                * Math.max(0, Math.min(1, qi / (float) maximum)));
        if (fillWidth > 0) graphics.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + 4, 0xFF8CB8D0);
    }

    private static HudLayout layout(Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean inventoryOpen = minecraft.screen instanceof InventoryScreen;
        int sideWidth = inventoryOpen ? (screenWidth - 176) / 2 : (screenWidth - 182) / 2;
        int availableWidth = sideWidth - 8;
        if (availableWidth < 48) {
            return null;
        }
        int panelWidth = Math.min(MAX_PANEL_WIDTH, availableWidth);
        int leftX = sideWidth - panelWidth - 4;
        int rightX = inventoryOpen ? (screenWidth + 176) / 2 + 4 : screenWidth - sideWidth + 4;
        int y = inventoryOpen ? screenHeight / 2 - PANEL_HEIGHT / 2 : screenHeight - PANEL_HEIGHT - 10;
        return new HudLayout(screenWidth, panelWidth, leftX, rightX, y);
    }

    private static boolean isArchiveButtonHovered(HudLayout layout, int mouseX, int mouseY) {
        if (layout.panelWidth() < 76) {
            return false;
        }
        int buttonX = layout.rightX() + layout.panelWidth() - 15;
        return mouseX >= buttonX && mouseX < buttonX + 12
                && mouseY >= layout.y() + 3 && mouseY < layout.y() + 15;
    }

    private static int guiMouseX(Minecraft minecraft) {
        return (int) (minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth());
    }

    private static int guiMouseY(Minecraft minecraft) {
        return (int) (minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight());
    }

    private record HudLayout(int screenWidth, int panelWidth, int leftX, int rightX, int y) {}
}
