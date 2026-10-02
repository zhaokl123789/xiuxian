package xiuxian.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xiuxian.client.CultivationClientState;
import xiuxian.cultivation.CultivationSpell;
import xiuxian.cultivation.CultivationSpells;
import xiuxian.network.XiuxianNetwork;

/** Detailed spell management screen. The inventory tooltip remains intentionally short. */
public final class SpellLoadoutScreen extends Screen {
    private static final int GOLD = 0xFFD4B46A;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int selectedSlot;
    private int selectedSpell;
    private int scroll;
    private List<CultivationSpell> visibleSpells = List.of();

    public SpellLoadoutScreen() {
        super(Component.literal("\u672f\u6cd5\u88c5\u914d"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(650, width - 18);
        panelHeight = Math.min(390, height - 18);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        List<CultivationSpell> spells = new ArrayList<>();
        for (CultivationSpell spell : CultivationSpells.all()) {
            boolean learned = CultivationSpells.isAutomaticallyLearned(spell.id())
                    || CultivationClientState.learnedSpellIds().contains(spell.id());
            boolean realm = CultivationClientState.realm().ordinal() >= spell.minimumRealm().ordinal()
                    && CultivationClientState.realm().ordinal() <= spell.maximumRealm().ordinal();
            boolean technique = spell.requiredTechniqueId() == null
                    || spell.requiredTechniqueId().equals(CultivationClientState.techniqueId());
            if (learned && realm && technique) spells.add(spell);
        }
        visibleSpells = List.copyOf(spells);
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            addRenderableWidget(Button.builder(Component.literal(slotLabel(slot)), button -> selectedSlot = slot)
                    .bounds(panelLeft + 18, panelTop + 52 + i * 30, 150, 24).build());
        }
        int listTop = panelTop + 48;
        int rows = Math.max(1, Math.min(9, (panelHeight - 100) / 28));
        int start = Math.min(scroll, Math.max(0, visibleSpells.size() - rows));
        for (int i = start; i < Math.min(visibleSpells.size(), start + rows); i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.literal(visibleSpells.get(i).displayName()), button -> selectedSpell = index)
                    .bounds(panelLeft + 182, listTop + (i - start) * 28, 146, 22).build());
        }
        addRenderableWidget(Button.builder(Component.literal("\u88c5\u5165\u9009\u4e2d\u672f\u6cd5"), button -> equipSelected())
                .bounds(panelLeft + 182, panelTop + panelHeight - 48, 146, 22).build());
        addRenderableWidget(Button.builder(Component.literal("\u6e05\u7a7a\u5f53\u524d\u69fd\u4f4d"), button -> {
            XiuxianNetwork.requestEquipSpell(selectedSlot, "");
            rebuildSpellWidgets();
        }).bounds(panelLeft + 18, panelTop + panelHeight - 48, 150, 22).build());
        addRenderableWidget(Button.builder(Component.literal("\u8fd4\u56de"), button -> onClose())
                .bounds(panelLeft + panelWidth - 84, panelTop + panelHeight - 28, 66, 20).build());
    }

    private void equipSelected() {
        if (selectedSpell >= 0 && selectedSpell < visibleSpells.size()) {
            XiuxianNetwork.requestEquipSpell(selectedSlot, visibleSpells.get(selectedSpell).id());
        }
    }

    private String slotLabel(int slot) {
        String id = CultivationClientState.spellAt(slot);
        CultivationSpell spell = CultivationSpells.byId(id);
        return (slot + 1) + ". " + (spell == null ? "\u7a7a\u69fd" : spell.displayName());
    }

    private void rebuildSpellWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xF0141916);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, GOLD);
        graphics.fill(panelLeft, panelTop + panelHeight - 2, panelLeft + panelWidth, panelTop + panelHeight, GOLD);
        graphics.drawCenteredString(font, "\u672f\u6cd5\u88c5\u914d", width / 2, panelTop + 12, GOLD);
        graphics.drawString(font, "\u5df2\u88c5\u914d\u69fd", panelLeft + 18, panelTop + 36, 0xFFE5DDCA, false);
        graphics.drawString(font, "\u53ef\u7528\u672f\u6cd5", panelLeft + 182, panelTop + 36, 0xFFE5DDCA, false);
        CultivationSpell spell = selectedSpell >= 0 && selectedSpell < visibleSpells.size()
                ? visibleSpells.get(selectedSpell) : null;
        int detailX = panelLeft + 344;
        graphics.fill(detailX, panelTop + 48, panelLeft + panelWidth - 18, panelTop + panelHeight - 38, 0xB81D2822);
        if (spell == null) {
            graphics.drawString(font, "\u70b9\u51fb\u53f3\u4fa7\u672f\u6cd5\u67e5\u770b\u8be6\u7ec6\u4fe1\u606f", detailX + 12, panelTop + 64, 0xFFB7C9BC, false);
        } else {
            graphics.drawString(font, spell.displayName(), detailX + 12, panelTop + 60, GOLD, false);
            graphics.drawString(font, "\u5c5e\u6027\uff1a" + spell.element().displayName(), detailX + 12, panelTop + 78, 0xFFE2DCCB, false);
            graphics.drawString(font, "\u771f\u6c14\u6d88\u8017\uff1a" + spell.trueQiCost() + "  \u51b7\u5374\uff1a" + spell.cooldownTicks() / 20 + "\u79d2", detailX + 12, panelTop + 94, 0xFFE2DCCB, false);
            graphics.drawString(font, "\u65bd\u6cd5\u8303\u56f4\uff1a" + spell.range() + "  \u65b9\u5f0f\uff1a" + spell.usage(), detailX + 12, panelTop + 110, 0xFFE2DCCB, false);
            int y = panelTop + 132;
            for (var line : font.split(Component.literal(spell.description()), panelWidth - 380)) {
                graphics.drawString(font, line, detailX + 12, y, 0xFFB7C9BC, false);
                y += 12;
            }
            String source = CultivationSpells.isAutomaticallyLearned(spell.id())
                    ? "\u80ce\u606f\u5883\u81ea\u52a8\u9886\u609f" : "\u5947\u9047\u672f\u6cd5\u4e66\u5df2\u9886\u609f";
            graphics.drawString(font, source, detailX + 12, panelTop + panelHeight - 58, 0xFF8FC3A5, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = Math.max(1, Math.min(9, (panelHeight - 100) / 28));
        int max = Math.max(0, visibleSpells.size() - rows);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        rebuildSpellWidgets();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
