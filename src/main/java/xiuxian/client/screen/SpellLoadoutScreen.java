package xiuxian.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import xiuxian.client.CultivationClientState;
import xiuxian.cultivation.CultivationSpell;
import xiuxian.cultivation.CultivationSpells;
import xiuxian.network.XiuxianNetwork;

/** A compact loadout board: choose a slot, then click a spell card to equip it immediately. */
public final class SpellLoadoutScreen extends Screen {
    private static final int PANEL = 0xF21B2821;
    private static final int PANEL_ALT = 0xD927382F;
    private static final int GOLD = 0xFFE0B968;
    private static final int TEXT = 0xFFE6DDC7;
    private static final int MUTED = 0xFF9EB0A1;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int selectedSlot;
    private int selectedSpell = -1;
    private int scroll;
    private List<CultivationSpell> visibleSpells = List.of();

    public SpellLoadoutScreen() {
        super(Component.literal("\u672f\u6cd5\u88c5\u914d"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(900, width - 20);
        panelHeight = Math.min(500, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        visibleSpells = availableSpells();

        int slotsLeft = panelLeft + 18;
        for (int slot = 0; slot < 4; slot++) {
            addRenderableWidget(new SlotButton(slotsLeft, panelTop + 64 + slot * 57, 214, 46, slot));
        }

        int listLeft = panelLeft + 248;
        int listTop = panelTop + 62;
        int listWidth = Math.max(210, Math.min(300, panelWidth / 3));
        int rows = Math.max(1, (panelHeight - 116) / 34);
        int start = Math.min(scroll, Math.max(0, visibleSpells.size() - rows));
        for (int i = start; i < Math.min(visibleSpells.size(), start + rows); i++) {
            addRenderableWidget(new SpellButton(listLeft, listTop + (i - start) * 34, listWidth, 28, i));
        }

        addRenderableWidget(new ClearButton(slotsLeft, panelTop + panelHeight - 45, 214, 27));
        addRenderableWidget(new CloseButton(panelLeft + panelWidth - 82, panelTop + panelHeight - 32, 64, 22));
    }

    private List<CultivationSpell> availableSpells() {
        List<CultivationSpell> result = new ArrayList<>();
        for (CultivationSpell spell : CultivationSpells.all()) {
            boolean learned = CultivationSpells.isAutomaticallyLearned(spell.id())
                    || CultivationClientState.learnedSpellIds().contains(spell.id());
            boolean realm = CultivationClientState.realm().ordinal() >= spell.minimumRealm().ordinal()
                    && CultivationClientState.realm().ordinal() <= spell.maximumRealm().ordinal();
            boolean technique = spell.requiredTechniqueId() == null
                    || spell.requiredTechniqueId().equals(CultivationClientState.techniqueId());
            if (learned && realm && technique) result.add(spell);
        }
        return List.copyOf(result);
    }

    private void equip(int spellIndex) {
        if (spellIndex < 0 || spellIndex >= visibleSpells.size()) return;
        selectedSpell = spellIndex;
        XiuxianNetwork.requestEquipSpell(selectedSlot, visibleSpells.get(spellIndex).id());
    }

    private void clearSlot() {
        XiuxianNetwork.requestEquipSpell(selectedSlot, "");
    }

    private String slotLabel(int slot) {
        String id = CultivationClientState.spellAt(slot);
        CultivationSpell spell = CultivationSpells.byId(id);
        return spell == null ? "\u7a7a\u69fd" : spell.displayName();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0x9A07100C);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 3, GOLD);
        graphics.fill(panelLeft, panelTop + panelHeight - 3, panelLeft + panelWidth, panelTop + panelHeight, GOLD);
        graphics.drawCenteredString(font, "\u592a\u865a\u672f\u6cd5\u8c31", width / 2, panelTop + 14, GOLD);
        graphics.drawCenteredString(font, "\u9009\u62e9\u5feb\u6377\u69fd\uff0c\u70b9\u51fb\u672f\u6cd5\u5361\u5373\u53ef\u88c5\u5165", width / 2, panelTop + 32, MUTED);

        int slotsLeft = panelLeft + 18;
        int listLeft = panelLeft + 248;
        int listWidth = Math.max(210, Math.min(300, panelWidth / 3));
        int detailLeft = listLeft + listWidth + 18;
        graphics.fill(slotsLeft, panelTop + 49, slotsLeft + 214, panelTop + panelHeight - 56, 0xB816211B);
        graphics.fill(listLeft - 8, panelTop + 49, listLeft + listWidth + 8, panelTop + panelHeight - 56, 0xB816211B);
        graphics.fill(detailLeft, panelTop + 49, panelLeft + panelWidth - 18, panelTop + panelHeight - 56, PANEL_ALT);
        graphics.drawString(font, "\u5feb\u6377\u69fd", slotsLeft + 12, panelTop + 55, TEXT, false);
        graphics.drawString(font, "\u53ef\u7528\u672f\u6cd5", listLeft + 4, panelTop + 55, TEXT, false);
        graphics.drawString(font, "\u672f\u6cd5\u8be6\u89e3", detailLeft + 12, panelTop + 55, TEXT, false);

        CultivationSpell selected = selectedSpell >= 0 && selectedSpell < visibleSpells.size()
                ? visibleSpells.get(selectedSpell) : null;
        if (selected == null) {
            graphics.drawString(font, "\u70b9\u51fb\u4e2d\u95f4\u672f\u6cd5\u5361\u88c5\u5165\u5f53\u524d\u69fd\u4f4d", detailLeft + 12, panelTop + 82, MUTED, false);
        } else {
            int x = detailLeft + 12;
            int y = panelTop + 82;
            graphics.drawString(font, selected.displayName(), x, y, GOLD, false);
            graphics.drawString(font, "\u5c5e\u6027\uff1a" + selected.element().displayName(), x, y + 20, TEXT, false);
            graphics.drawString(font, "\u6d88\u8017\uff1a" + selected.trueQiCost() + " \u771f\u6c14", x, y + 37, TEXT, false);
            graphics.drawString(font, "\u51b7\u5374\uff1a" + Math.max(1, selected.cooldownTicks() / 20) + " \u79d2", x, y + 54, TEXT, false);
            graphics.drawString(font, "\u8303\u56f4\uff1a" + selected.range() + " \u683c  \u65bd\u6cd5\uff1a" + selected.usage(), x, y + 71, TEXT, false);
            int lineY = y + 98;
            for (FormattedCharSequence line : font.split(Component.literal(selected.description()), panelLeft + panelWidth - detailLeft - 42)) {
                graphics.drawString(font, line, x, lineY, MUTED, false);
                lineY += 13;
            }
            graphics.drawString(font, CultivationSpells.isAutomaticallyLearned(selected.id())
                    ? "\u83b7\u53d6\uff1a\u80ce\u606f\u5165\u5883\u81ea\u609f" : "\u83b7\u53d6\uff1a\u5947\u9047\u672f\u6cd5\u4e66", x, panelTop + panelHeight - 76, 0xFF9BCBA4, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = Math.max(1, (panelHeight - 116) / 34);
        int max = Math.max(0, visibleSpells.size() - rows);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        rebuild();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private abstract class RuneButton extends AbstractButton {
        RuneButton(int x, int y, int width, int height, Component message) {
            super(x, y, width, height, message);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int fill = isHoveredOrFocused() ? 0xE0526A55 : 0xD12A3930;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            graphics.renderOutline(getX(), getY(), width, height, isFocused() ? GOLD : 0xFF667A65);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2,
                    getY() + (height - 8) / 2, TEXT);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class SlotButton extends RuneButton {
        private final int slot;

        SlotButton(int x, int y, int width, int height, int slot) {
            super(x, y, width, height, Component.literal((slot + 1) + "  " + slotLabel(slot)));
            this.slot = slot;
        }

        @Override
        public void onPress() {
            selectedSlot = slot;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int fill = selectedSlot == slot ? 0xE08C6B3E : (isHoveredOrFocused() ? 0xE0526A55 : 0xD12A3930);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            graphics.renderOutline(getX(), getY(), width, height, selectedSlot == slot ? GOLD : 0xFF667A65);
            graphics.drawString(font, (slot + 1) + "", getX() + 12, getY() + 16, GOLD, false);
            graphics.drawString(font, slotLabel(slot), getX() + 38, getY() + 16, TEXT, false);
        }
    }

    private final class SpellButton extends RuneButton {
        private final int index;

        SpellButton(int x, int y, int width, int height, int index) {
            super(x, y, width, height, Component.literal(visibleSpells.get(index).displayName()));
            this.index = index;
        }

        @Override
        public void onPress() {
            equip(index);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int fill = selectedSpell == index ? 0xE08C6B3E : (isHoveredOrFocused() ? 0xE0526A55 : 0xD12A3930);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            graphics.renderOutline(getX(), getY(), width, height, selectedSpell == index ? GOLD : 0xFF667A65);
            CultivationSpell spell = visibleSpells.get(index);
            graphics.drawString(font, spell.displayName(), getX() + 10, getY() + 5, TEXT, false);
            graphics.drawString(font, spell.element().displayName() + "  " + spell.trueQiCost() + "\u771f\u6c14",
                    getX() + 10, getY() + 16, MUTED, false);
        }
    }

    private final class ClearButton extends RuneButton {
        ClearButton(int x, int y, int width, int height) {
            super(x, y, width, height, Component.literal("\u6e05\u7a7a\u9009\u4e2d\u69fd\u4f4d"));
        }

        @Override
        public void onPress() {
            clearSlot();
        }
    }

    private final class CloseButton extends RuneButton {
        CloseButton(int x, int y, int width, int height) {
            super(x, y, width, height, Component.literal("\u8fd4\u56de"));
        }

        @Override
        public void onPress() {
            onClose();
        }
    }
}
