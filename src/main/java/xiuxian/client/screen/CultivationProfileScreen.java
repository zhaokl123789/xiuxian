package xiuxian.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xiuxian.client.CultivationClientState;
import xiuxian.cultivation.CultivationRealm;
import xiuxian.cultivation.CultivationTechnique;
import xiuxian.cultivation.CultivationTechniques;

public class CultivationProfileScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 390;
    private static final int GOLD = 0xFFD4B46A;
    private static final String[] ATTRIBUTE_NAMES = {"灵根", "根骨", "悟性", "气运"};
    private final Screen parent;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private float layoutScale;
    private int selectedAttribute = -1;

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
        if (CultivationClientState.isInitialized()) {
            for (int i = 0; i < ATTRIBUTE_NAMES.length; i++) {
                final int attributeIndex = i;
                addRenderableWidget(Button.builder(Component.literal("?"), button -> selectedAttribute = attributeIndex)
                .bounds(panelLeft + 53, panelTop + scaled(240 + i * 20), 18, scaled(15))
                        .tooltip(Tooltip.create(Component.literal("查看" + ATTRIBUTE_NAMES[i] + "的作用")))
                        .build());
            }
        }

        int actionY = panelTop + panelHeight - scaled(27);
        int actionWidth = (panelWidth - 48) / 2;
        CultivationTechnique technique = CultivationTechniques.byId(CultivationClientState.techniqueId());
        CultivationRealm currentRealm = CultivationClientState.realm();
        CultivationRealm targetRealm = CultivationClientState.realmLevel() == currentRealm.levelCount()
                ? currentRealm.next() : currentRealm;
        boolean atRealmCap = targetRealm == null;
        boolean foundationBlocked = targetRealm == CultivationRealm.PURPLE_MANSION && technique != null
                && !technique.matchesImmortalFoundation(CultivationClientState.immortalFoundation());
        boolean techniqueBlocked = !atRealmCap && (technique == null
                || !technique.canBeLearnedAt(currentRealm) || !technique.canCultivateTo(targetRealm)
                || foundationBlocked);
        boolean majorBreakthrough = CultivationClientState.realmLevel() == CultivationClientState.realm().levelCount();
        boolean enoughQi = CultivationClientState.breakthroughCost() > 0
                && CultivationClientState.qi() >= CultivationClientState.breakthroughCost();
        String breakthroughLabel = atRealmCap ? "此境已圆满"
                : techniqueBlocked ? "需参悟更高阶功法"
                : enoughQi ? (majorBreakthrough
                ? "冲击大境界 · " + CultivationClientState.breakthroughChance() + "%" : "突破小境界")
                : "还需 " + Math.max(0, CultivationClientState.breakthroughCost() - CultivationClientState.qi()) + " 点修为";
        Button.Builder breakthroughBuilder = Button.builder(Component.literal(breakthroughLabel), button -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.connection.sendCommand("xiuxian breakthrough");
                        Minecraft.getInstance().setScreen(null);
                    }
                })
                .bounds(panelLeft + 18, actionY, actionWidth, scaled(20));
        if (techniqueBlocked) {
            String reason = foundationBlocked
                    ? "当前仙基为" + CultivationClientState.immortalFoundation()
                    + "，需修习契基或生扶仙基的功法，方可冲击紫府。"
                    : "当前功法最高适修至" + (technique == null ? "未知境界" : technique.maximumRealm().displayName())
                    + "，参悟新功法后可继续突破。";
            breakthroughBuilder.tooltip(Tooltip.create(Component.literal(reason)));
        } else if (majorBreakthrough && !atRealmCap) {
            int fatalRisk = currentRealm.fatalBreakthroughRiskChance(
                    CultivationClientState.majorBreakthroughFailures());
            String riskText = fatalRisk > 0 ? "；失败陨落风险 " + fatalRisk + "%" : "；失败不危及性命";
            breakthroughBuilder.tooltip(Tooltip.create(Component.literal("本次成功率 "
                    + CultivationClientState.breakthroughChance() + "%；失败会损耗修为并降低后续成功率"
                    + riskText + "。")));
        }
        Button breakthrough = breakthroughBuilder.build();
        breakthrough.active = !atRealmCap && !techniqueBlocked && enoughQi;
        addRenderableWidget(breakthrough);
        addRenderableWidget(Button.builder(Component.literal("返回"), button -> onClose())
                .bounds(panelLeft + 30 + actionWidth, actionY, actionWidth, scaled(20)).build());
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

        drawValueRow(graphics, "境界", CultivationClientState.realm().displayName() + " · "
                + CultivationClientState.realm().stageLabel(CultivationClientState.realmLevel()), panelTop + scaled(52));
        CultivationTechnique technique = CultivationTechniques.byId(CultivationClientState.techniqueId());
        String techniqueValue = technique == null ? "未识功法"
                : technique.displayName() + " · " + technique.combatStyle();
        drawValueRow(graphics, "功法战式", techniqueValue, panelTop + scaled(68));
        int trueQiRecovery = CultivationClientState.passiveTrueQiRecoveryPerTenSeconds();
        String trueQiValue = CultivationClientState.trueQi() + "/" + CultivationClientState.trueQiMaximum()
                + (trueQiRecovery > 0 ? " · 每10秒 +" + trueQiRecovery : "");
        drawValueRow(graphics, "真炁", trueQiValue, panelTop + scaled(84));
        String affinityValue = technique == null ? "无有效功法"
                : CultivationClientState.immortalFoundation().isBlank()
                ? technique.realmRangeLabel() + " · " + technique.fiveVirtue()
                : CultivationClientState.immortalFoundation() + "仙基 · 当前" + technique.elementalAffinity() + "炁";
        drawValueRow(graphics, CultivationClientState.immortalFoundation().isBlank() ? "适修与炁性" : "仙基与炁性",
                affinityValue, panelTop + scaled(96));
        int healthRecoveryInterval = CultivationClientState.passiveHealthRecoveryIntervalTicks();
        String healthValue = playerHealth() + (healthRecoveryInterval > 0
                ? " · 每" + String.format(java.util.Locale.ROOT, "%.1f", healthRecoveryInterval / 20.0D) + "秒 +1"
                : " · 尚未通脉");
        drawValueRow(graphics, "气血", healthValue, panelTop + scaled(108));
        drawValueRow(graphics, "攻击", playerAttack(), panelTop + scaled(124));
        drawValueRow(graphics, "护甲", playerArmor(), panelTop + scaled(140));
        drawValueRow(graphics, "境界增益", realmBonuses(), panelTop + scaled(156));
        drawValueRow(graphics, "修为", CultivationClientState.qi() + "/"
                + CultivationClientState.breakthroughCost(), panelTop + scaled(172));
        CultivationRealm currentRealm = CultivationClientState.realm();
        CultivationRealm targetRealm = CultivationClientState.realmLevel() == currentRealm.levelCount()
                ? currentRealm.next() : currentRealm;
        boolean atRealmCap = targetRealm == null;
        boolean foundationBlocked = targetRealm == CultivationRealm.PURPLE_MANSION && technique != null
                && !technique.matchesImmortalFoundation(CultivationClientState.immortalFoundation());
        boolean techniqueBlocked = !atRealmCap && (technique == null
                || !technique.canBeLearnedAt(currentRealm) || !technique.canCultivateTo(targetRealm)
                || foundationBlocked);
        boolean majorBreakthrough = CultivationClientState.realmLevel() == currentRealm.levelCount();
        String chance = atRealmCap ? "已至体系上限" : techniqueBlocked
                ? (foundationBlocked ? "需功法契基" : "需更高阶功法")
                : majorBreakthrough ? CultivationClientState.breakthroughChance() + "%"
                + (CultivationClientState.majorBreakthroughFailures() > 0
                ? " · 连败 " + CultivationClientState.majorBreakthroughFailures() : "") : "小境界必成";
        drawValueRow(graphics, "突破把握", chance, panelTop + scaled(188));
        drawProgressBar(graphics, panelLeft + 82, panelTop + scaled(201), panelWidth - 100,
                CultivationClientState.qi(), CultivationClientState.breakthroughCost(), 0xFF70A98C);
        drawValueRow(graphics, "炼丹师", "等级 " + CultivationClientState.alchemyLevel() + " · 经验 "
                + CultivationClientState.alchemyExperience() + "/"
                + CultivationClientState.alchemyExperienceToNextLevel(), panelTop + scaled(210));
        String movement = switch (CultivationClientState.realm()) {
            case FETAL_BREATH -> "疾走加速 · 耗真炁";
            case QI_REFINING -> "疾走、腾跃 · 耗真炁";
            case FOUNDATION_ESTABLISHMENT -> "疾走、腾跃、御空 · 耗真炁";
            case PURPLE_MANSION -> "御空身法 · V 太虚步";
            default -> "当前境界未开放";
        };
        drawValueRow(graphics, "行炁身法", movement, panelTop + scaled(222));

        graphics.fill(panelLeft + 18, panelTop + scaled(234), panelLeft + panelWidth - 18, panelTop + scaled(235), 0x997E6842);
        graphics.drawString(this.font, "修行资质", panelLeft + 20, panelTop + scaled(239), GOLD, false);
        drawAttribute(graphics, ATTRIBUTE_NAMES[0], CultivationClientState.spiritualRoot(), panelTop + scaled(251));
        drawAttribute(graphics, ATTRIBUTE_NAMES[1], CultivationClientState.constitution(), panelTop + scaled(271));
        drawAttribute(graphics, ATTRIBUTE_NAMES[2], CultivationClientState.comprehension(), panelTop + scaled(291));
        drawAttribute(graphics, ATTRIBUTE_NAMES[3], CultivationClientState.fortune(), panelTop + scaled(311));

        if (selectedAttribute >= 0 && selectedAttribute < ATTRIBUTE_NAMES.length) {
            graphics.fill(panelLeft + 18, panelTop + scaled(325), panelLeft + panelWidth - 18,
                    panelTop + scaled(326), 0x997E6842);
            graphics.drawString(this.font, ATTRIBUTE_NAMES[selectedAttribute], panelLeft + 20,
                    panelTop + scaled(332), GOLD, false);
            String description = attributeDescription(selectedAttribute, technique);
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                    Component.literal(description), panelWidth - 50);
            int y = panelTop + scaled(342);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(this.font, lines.get(i), panelLeft + 20, y + scaled(i * 10),
                        0xFFE2DCCB, false);
            }
        }

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

    private String playerAttack() {
        Player player = Minecraft.getInstance().player;
        return player == null ? "--" : String.format(java.util.Locale.ROOT, "%.1f", player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private String playerArmor() {
        Player player = Minecraft.getInstance().player;
        return player == null ? "--" : Integer.toString(player.getArmorValue());
    }

    private String realmBonuses() {
        CultivationRealm realm = CultivationClientState.realm();
        int level = CultivationClientState.realmLevel();
        return String.format(java.util.Locale.ROOT, "+%.0f血 · +%.1f攻 · 减伤%.0f%%",
                realm.healthBonusAt(level), realm.attackBonusAt(level), realm.damageReductionAt(level) * 100.0F);
    }

    private String attributeDescription(int index, CultivationTechnique technique) {
        String detail = switch (index) {
            case 0 -> "契合当前功法时提升吐纳效率：适配系数为 80% + 灵根/2，上限 130%。";
            case 1 -> "每点根骨降低 0.25% 受伤；根骨契合的功法也会获得吐纳适配。";
            case 2 -> "每点悟性降低约 0.5% 突破需求；每 5 点参悟成功率提高 1%。";
            case 3 -> "每 4 点气运使每秒吐纳多 1 毫点；参悟成功率每 10 点提高 1%。";
            default -> "";
        };
        if (technique != null && technique.meditationAptitude().ordinal() == index) {
            detail += "当前功法契合度 " + technique.aptitudeMatchPercent(
                    CultivationClientState.spiritualRoot(), CultivationClientState.constitution(),
                    CultivationClientState.comprehension(), CultivationClientState.fortune()) + "%。";
        }
        return detail;
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
        int valueStart = panelLeft + 96;
        int valueRight = panelLeft + panelWidth - 20;
        String visibleValue = fitText(value, valueRight - valueStart);
        graphics.drawString(this.font, visibleValue, valueRight - this.font.width(visibleValue), y,
                0xFFE9E1CE, false);
    }

    private String fitText(String value, int maxWidth) {
        if (this.font.width(value) <= maxWidth) return value;
        String ellipsis = "…";
        int textWidth = maxWidth - this.font.width(ellipsis);
        return textWidth <= 0 ? "" : this.font.plainSubstrByWidth(value, textWidth) + ellipsis;
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
