package xiuxian.client.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import xiuxian.client.CultivationClientState;
import xiuxian.cultivation.CultivationTechnique;
import xiuxian.cultivation.CultivationTechniques;

public final class TechniqueBookScreen extends Screen {
    private static final int GOLD = 0xFFD4B46A;
    private final String techniqueId;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int scrollOffset;

    public TechniqueBookScreen(String techniqueId) {
        super(Component.literal("\u4fee\u884c\u5178\u7c4d"));
        this.techniqueId = techniqueId;
    }

    @Override
    protected void init() {
        panelWidth = Math.max(200, Math.min(420, width - 24));
        panelHeight = Math.max(180, Math.min(360, height - 24));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0181A18);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, GOLD);
        graphics.fill(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, GOLD);
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelHeight, 0xFF78633D);
        graphics.fill(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF78633D);

        if (technique == null) {
            graphics.drawCenteredString(font, "\u5178\u7c4d\u6b8b\u5377", width / 2, panelY + 30, 0xFFE5DDCA);
        } else {
            graphics.drawCenteredString(font, "\u300a" + technique.displayName() + "\u300b", width / 2,
                    panelY + 15, GOLD);
            graphics.drawCenteredString(font, technique.fiveVirtue() + " \u00b7 " + technique.qiAffinity(),
                    width / 2, panelY + 34, 0xFFAAC6B5);
            graphics.fill(panelX + 20, panelY + 53, panelX + panelWidth - 20, panelY + 54, 0x997E6842);

            int viewportTop = panelY + 57;
            int viewportBottom = panelY + panelHeight - 34;
            graphics.enableScissor(panelX + 10, viewportTop, panelX + panelWidth - 10, viewportBottom);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0D, -scrollOffset, 0.0D);
            int y = panelY + 64;
            y = drawSection(graphics, "\u9053\u8bba", technique.doctrine(), y);
            y = drawSection(graphics, "\u884c\u529f\u6cd5\u95e8", technique.method(), y + 5);
            String combat = "\u6218\u6cd5\uff1a" + technique.combatStyle()
                    + "\uff1b\u771f\u7081\u4e0a\u9650 +" + technique.trueQiBonus()
                    + "\uff1b\u6bcf\u79d2\u56de\u590d " + technique.trueQiRecoveryPerSecond()
                    + "\uff1b\u672f\u6cd5\u6548\u80fd \u00d7"
                    + String.format(java.util.Locale.ROOT, "%.2f", technique.spellPowerMultiplier())
                    + "\uff1b\u8fd1\u6218\u653b\u51fb +" + technique.combatAttackBonus();
            y = drawSection(graphics, "\u771f\u7081\u4e0e\u6218\u6cd5", combat, y + 5);
            String relation = technique.relationSummary()
                    + "\uff1b\u540c\u7cfb\u53c2\u609f\u589e\u76ca\uff1a\u5410\u7eb3 +" + technique.resonanceMeditationBonusPercent() + "%"
                    + "\uff0c\u771f\u7081\u6062\u590d +" + technique.resonanceTrueQiBonus()
                    + "\uff1b\u8fd0\u8f6c\u51cf\u76ca\uff1a\u5410\u7eb3 -" + technique.drawbackMeditationPercent() + "%"
                    + "\uff0c\u771f\u7081\u6062\u590d\u4ee3\u4ef7 " + technique.drawbackTrueQiCostPercent() + "%";
            y = drawSection(graphics, "\u529f\u6cd5\u8054\u7cfb", relation, y + 5);
            String inheritance = "\u9002\u4fee\u5883\u754c\uff1a" + technique.realmRangeLabel()
                    + "\uff1b\u53c2\u609f\u96be\u5ea6\uff1a" + technique.learningDifficulty() + "/10 \u00b7 "
                    + technique.learningDifficultyLabel()
                    + "\u3002\u96be\u5ea6\u4f1a\u5f71\u54cd\u4e60\u5f97\u51e0\u7387\u4e0e\u4fee\u884c\u6548\u7387\u3002"
                    + "\u5f53\u524d\u529f\u6cd5\uff1a"
                    + (CultivationClientState.techniqueId().equals(technique.id())
                    ? "\u5df2\u4fee\u4e60" : "\u5c1a\u672a\u4fee\u4e60");
            y = drawSection(graphics, "\u5883\u754c\u4e0e\u4f20\u627f", inheritance, y + 5);
            graphics.pose().popPose();
            graphics.disableScissor();

            int maxScroll = Math.max(0, y - viewportBottom + 4);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            String instruction = "\u6eda\u8f6e\u9605\u8bfb \u00b7 \u6536\u5377\u540e\u8e72\u4e0b\u5e76\u53f3\u952e\u6b64\u4e66\u53c2\u609f";
            graphics.drawCenteredString(font, instruction, width / 2, panelY + panelHeight - 23, 0xFFE8D8AF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawSection(GuiGraphics graphics, String heading, String body, int y) {
        graphics.drawString(font, heading, panelX + 20, y, GOLD, false);
        List<FormattedCharSequence> lines = font.split(Component.literal(body), panelWidth - 40);
        int lineY = y + 13;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, panelX + 20, lineY, 0xFFE2DCCB, false);
            lineY += 11;
        }
        return lineY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(delta) * 14);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
