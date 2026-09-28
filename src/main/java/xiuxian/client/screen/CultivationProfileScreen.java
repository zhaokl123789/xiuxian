package xiuxian.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import xiuxian.client.CultivationClientState;

public class CultivationProfileScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 252;
    private static final int GOLD = 0xFFD4B46A;
    private final Screen parent;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private float layoutScale;

    public CultivationProfileScreen(Screen parent) {
        super(Component.literal("修行档案"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
        panelHeight = Math.min(PANEL_HEIGHT, this.height - 16);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;
        layoutScale = panelHeight / (float) PANEL_HEIGHT;
        addRenderableWidget(Button.builder(Component.literal("返回物品栏"), button -> onClose())
                .bounds(this.width / 2 - 52, panelTop + panelHeight - scaled(27), 104, scaled(20)).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB800090D);
        drawPanel(graphics);

        graphics.drawCenteredString(this.font, "修行档案", this.width / 2, panelTop + scaled(11), GOLD);
        if (!CultivationClientState.isInitialized()) {
            graphics.drawCenteredString(this.font, "尚未踏入修行之路", this.width / 2, panelTop + scaled(64), 0xFFE5DDCA);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        String identity = "人族 · " + CultivationClientState.familyOrigin().displayName()
                + " · " + CultivationClientState.cultivationPath().displayName();
        graphics.drawCenteredString(this.font, identity, this.width / 2, panelTop + scaled(29), 0xFFE2DCCB);
        graphics.fill(panelLeft + 18, panelTop + scaled(43), panelLeft + panelWidth - 18, panelTop + scaled(44), 0x997E6842);

        drawValueRow(graphics, "境界", CultivationClientState.realm().displayName()
                + " · 第 " + CultivationClientState.realmLevel() + " 层", panelTop + scaled(52));
        drawValueRow(graphics, "功法", "吐纳引气诀", panelTop + scaled(68));
        drawValueRow(graphics, "气血", playerHealth(), panelTop + scaled(84));
        drawValueRow(graphics, "修为", CultivationClientState.qi() + "/"
                + CultivationClientState.breakthroughCost(), panelTop + scaled(100));
        drawProgressBar(graphics, panelLeft + 82, panelTop + scaled(113), panelWidth - 100,
                CultivationClientState.qi(), CultivationClientState.breakthroughCost(), 0xFF70A98C);

        graphics.fill(panelLeft + 18, panelTop + scaled(124), panelLeft + panelWidth - 18, panelTop + scaled(125), 0x997E6842);
        graphics.drawString(this.font, "四象资质", panelLeft + 20, panelTop + scaled(132), GOLD, false);
        drawAttribute(graphics, "灵根", CultivationClientState.spiritualRoot(), panelTop + scaled(148));
        drawAttribute(graphics, "根骨", CultivationClientState.constitution(), panelTop + scaled(168));
        drawAttribute(graphics, "悟性", CultivationClientState.comprehension(), panelTop + scaled(188));
        drawAttribute(graphics, "气运", CultivationClientState.fortune(), panelTop + scaled(208));

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String playerHealth() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return "--";
        }
        return Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth());
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xF0181A18);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, GOLD);
        graphics.fill(panelLeft, panelTop + panelHeight - 1, panelLeft + panelWidth, panelTop + panelHeight, GOLD);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelTop + panelHeight, 0xFF78633D);
        graphics.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF78633D);
    }

    private void drawValueRow(GuiGraphics graphics, String label, String value, int y) {
        graphics.drawString(this.font, label, panelLeft + 20, y, 0xFFC9C3B5, false);
        graphics.drawString(this.font, value, panelLeft + panelWidth - 20 - this.font.width(value), y,
                0xFFE9E1CE, false);
    }

    private void drawAttribute(GuiGraphics graphics, String label, int value, int y) {
        int barX = panelLeft + 80;
        int barWidth = panelWidth - 160;
        String score = value + "/100";
        graphics.drawString(this.font, label, panelLeft + 20, y, 0xFFE1D9C5, false);
        graphics.drawString(this.font, score, panelLeft + panelWidth - 20 - this.font.width(score), y,
                0xFFC9C3B5, false);
        drawProgressBar(graphics, barX, y + scaled(10), barWidth, value, 100, 0xFF70A98C);
    }

    private void drawProgressBar(GuiGraphics graphics, int x, int y, int width,
                                 int value, int maximum, int color) {
        graphics.fill(x, y, x + width, y + 4, 0xFF161916);
        int fillWidth = maximum <= 0 ? 0 : Math.round((width - 2) * Math.max(0,
                Math.min(1, value / (float) maximum)));
        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + 3, color);
        }
    }

    private int scaled(int value) {
        return Math.round(value * layoutScale);
    }
}
