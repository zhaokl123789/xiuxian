package xiuxian.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xiuxian.cultivation.CultivationAttributeBonuses;
import xiuxian.cultivation.CultivationData;
import xiuxian.cultivation.CultivationPath;
import xiuxian.cultivation.FamilyOrigin;
import xiuxian.network.XiuxianNetwork;

/** First-login identity selection. The server still owns the final choice. */
public final class IdentityCreationScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 300;
    private static final int GOLD = 0xFFD6B66E;
    private static final int MUTED_GOLD = 0xFF8B7344;
    private FamilyOrigin selectedFamily;
    private CultivationPath selectedPath;
    private int panelLeft, panelTop, panelWidth, panelHeight;
    private float layoutScale;

    public IdentityCreationScreen() { super(Component.literal("\u9009\u62e9\u4fee\u884c\u8eab\u4efd")); }

    @Override
    protected void init() {
        panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
        panelLeft = (this.width - panelWidth) / 2;
        panelHeight = Math.min(PANEL_HEIGHT, this.height - 16);
        panelTop = (this.height - panelHeight) / 2;
        layoutScale = panelHeight / (float) PANEL_HEIGHT;
        int innerLeft = panelLeft + 16;
        int gap = 12;
        int columnWidth = (panelWidth - 32 - gap) / 2;
        int optionTop = panelTop + scaled(79);
        int optionHeight = scaled(22);
        int optionStep = scaled(28);
        int index = 0;
        for (FamilyOrigin family : FamilyOrigin.values()) {
            int row = index++;
            addRenderableWidget(Button.builder(optionLabel(family.displayName(), selectedFamily == family), button -> {
                selectedFamily = family; rebuildWidgets();
            }).tooltip(Tooltip.create(Component.literal(family.bonuses().summary())))
                    .bounds(innerLeft, optionTop + row * optionStep, columnWidth, optionHeight).build());
        }
        index = 0;
        for (CultivationPath path : CultivationPath.values()) {
            int row = index++;
            addRenderableWidget(Button.builder(optionLabel(path.displayName(), selectedPath == path), button -> {
                selectedPath = path; rebuildWidgets();
            }).tooltip(Tooltip.create(Component.literal(path.bonuses().summary())))
                    .bounds(innerLeft + columnWidth + gap, optionTop + row * optionStep,
                            columnWidth, optionHeight).build());
        }
        Button confirm = Button.builder(Component.literal("\u786e\u8ba4\u8eab\u4efd"), button -> {
            if (selectedFamily != null && selectedPath != null) {
                XiuxianNetwork.selectIdentity(selectedFamily.id(), selectedPath.id());
                button.active = false;
            }
        }).bounds(this.width / 2 - 65, panelTop + panelHeight - scaled(31), 130, optionHeight).build();
        confirm.active = selectedFamily != null && selectedPath != null;
        addRenderableWidget(confirm);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB800090D);
        drawPanel(graphics, panelLeft, panelTop, panelWidth, panelHeight);
        graphics.drawCenteredString(this.font, "\u9009\u62e9\u4fee\u884c\u8eab\u4efd", this.width / 2, panelTop + scaled(14), GOLD);
        graphics.drawCenteredString(this.font, "\u9009\u62e9\u51fa\u8eab\u4e0e\u9053\u9014\uff0c\u8e0f\u5165\u592a\u865a", this.width / 2, panelTop + scaled(34), 0xFFE8E0CF);
        graphics.fill(panelLeft + 24, panelTop + scaled(59), panelLeft + panelWidth - 24,
                panelTop + scaled(60), MUTED_GOLD);
        int innerLeft = panelLeft + 16;
        int gap = 12;
        int columnWidth = (panelWidth - 32 - gap) / 2;
        graphics.drawCenteredString(this.font, "\u5bb6\u65cf\u51fa\u8eab", innerLeft + columnWidth / 2,
                panelTop + scaled(65), GOLD);
        graphics.drawCenteredString(this.font, "\u4fee\u884c\u9053\u9014", innerLeft + columnWidth + gap + columnWidth / 2,
                panelTop + scaled(65), GOLD);
        graphics.drawCenteredString(this.font, "\u51fa\u8eab\u4e0e\u9053\u9014\u5c06\u5171\u540c\u51b3\u5b9a\u521d\u59cb\u5c5e\u6027\u4e0e\u4f20\u627f", this.width / 2,
                panelTop + scaled(169), 0xFFC8C3B6);
        drawAttributePreview(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    private Component optionLabel(String name, boolean selected) {
        return Component.literal(selected ? "\u25c6 " + name : name);
    }

    private void drawAttributePreview(GuiGraphics graphics) {
        CultivationAttributeBonuses bonuses = CultivationAttributeBonuses.NEUTRAL;
        if (selectedFamily != null) bonuses = bonuses.plus(selectedFamily.bonuses());
        if (selectedPath != null) bonuses = bonuses.plus(selectedPath.bonuses());
        int top = panelTop + scaled(190);
        graphics.drawCenteredString(this.font, "\u521d\u59cb\u5c5e\u6027\uff08\u786e\u8ba4\u540e\u968f\u673a\u53d6\u503c\uff09", this.width / 2, top, GOLD);
        graphics.drawCenteredString(this.font, "\u7075\u6839 " + attributeRange(bonuses.spiritualRoot())
                + "    \u4f53\u9b44 " + attributeRange(bonuses.constitution()), this.width / 2,
                top + scaled(13), 0xFFE1D9C5);
        graphics.drawCenteredString(this.font, "\u609f\u6027 " + attributeRange(bonuses.comprehension())
                + "    \u798f\u7f18 " + attributeRange(bonuses.fortune()), this.width / 2,
                top + scaled(26), 0xFFE1D9C5);
        String kit = selectedFamily == null || selectedPath == null
                ? "\u8bf7\u9009\u62e9\u51fa\u8eab\u4e0e\u9053\u9014\u67e5\u770b\u521d\u59cb\u9988\u8d60"
                : selectedFamily.startingKitSummary(selectedPath);
        java.util.List<net.minecraft.util.FormattedCharSequence> kitLines = this.font.split(
                Component.literal("\u521d\u59cb\u9988\u8d60\uff1a" + kit), panelWidth - 36);
        for (int i = 0; i < Math.min(2, kitLines.size()); i++) {
            graphics.drawCenteredString(this.font, kitLines.get(i), this.width / 2,
                    top + scaled(42 + i * 10), 0xFFC9C3B5);
        }
    }

    private String attributeRange(int bonus) {
        int minimum = Math.max(1, CultivationData.BASE_ATTRIBUTE_MIN + bonus);
        int maximum = Math.min(CultivationData.MAX_ATTRIBUTE_SCORE, CultivationData.BASE_ATTRIBUTE_MAX + bonus);
        return minimum + "-" + maximum;
    }

    private int scaled(int value) { return Math.round(value * layoutScale); }

    private void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, 0xF0181A18);
        graphics.fill(left, top, left + width, top + 1, GOLD);
        graphics.fill(left, top + height - 1, left + width, top + height, GOLD);
        graphics.fill(left, top, left + 1, top + height, MUTED_GOLD);
        graphics.fill(left + width - 1, top, left + width, top + height, MUTED_GOLD);
        graphics.fill(left + 8, top + 4, left + width - 8, top + 5, 0x553F775E);
    }
}
