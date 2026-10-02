package xiuxian.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import xiuxian.client.CultivationClientState;
import xiuxian.cultivation.CultivationSpell;
import xiuxian.cultivation.CultivationSpells;
import xiuxian.network.XiuxianNetwork;

/** Drag-and-drop spell board with an adaptive details panel. */
public final class SpellLoadoutScreen extends Screen {
    private static final int PANEL = 0xF21A2420;
    private static final int PANEL_DARK = 0xD915201C;
    private static final int PANEL_LIGHT = 0xD92A3930;
    private static final int GOLD = 0xFFE0B968;
    private static final int TEXT = 0xFFE8E0CC;
    private static final int MUTED = 0xFF9EB0A1;
    private static final int GREEN = 0xFF8FCDA4;

    private int panelLeft, panelTop, panelWidth, panelHeight;
    private int slotsLeft, slotsTop, slotsWidth, listLeft, listTop, listWidth;
    private int detailLeft, detailWidth, contentBottom, detailTop, detailHeight;
    private int scroll, detailScroll, selectedSlot, selectedSpell = -1, draggingSpell = -1;
    private double dragX, dragY;
    private boolean dragging, compactLayout;
    private List<CultivationSpell> visibleSpells = List.of();

    public SpellLoadoutScreen() { super(Component.literal("\u672f\u6cd5\u88c5\u914d")); }

    @Override
    protected void init() {
        panelWidth = Math.min(1120, Math.max(300, width - 24));
        panelHeight = Math.min(650, Math.max(260, height - 24));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        slotsLeft = panelLeft + 18;
        slotsTop = panelTop + 83;
        listTop = slotsTop;
        contentBottom = panelTop + panelHeight - 18;
        compactLayout = panelWidth < 720;
        int innerWidth = panelWidth - 36;
        if (compactLayout) {
            slotsWidth = Math.max(120, (innerWidth - 16) * 36 / 100);
            listLeft = slotsLeft + slotsWidth + 16;
            listWidth = Math.max(120, innerWidth - slotsWidth - 16);
            detailLeft = panelLeft + 18;
            detailWidth = innerWidth;
            detailHeight = Math.min(172, Math.max(148, panelHeight / 3));
            detailTop = panelTop + panelHeight - detailHeight - 18;
            contentBottom = detailTop - 10;
        } else {
            int gap = 16;
            detailWidth = Math.max(250, Math.min(370, innerWidth * 30 / 100));
            slotsWidth = Math.max(180, Math.min(270, innerWidth * 23 / 100));
            listWidth = innerWidth - slotsWidth - detailWidth - gap * 2;
            if (listWidth < 220) {
                detailWidth = Math.max(240, detailWidth - (220 - listWidth));
                listWidth = innerWidth - slotsWidth - detailWidth - gap * 2;
            }
            listLeft = slotsLeft + slotsWidth + gap;
            detailLeft = listLeft + listWidth + gap;
            detailTop = panelTop + 60;
            detailHeight = panelHeight - 78;
        }
        visibleSpells = availableSpells();
        scroll = Math.min(scroll, maxScroll());
        detailScroll = Math.max(0, detailScroll);
        selectedSlot = Math.max(0, Math.min(CultivationClientState.spellSlotCount() - 1, selectedSlot));
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

    private int cardHeight() { return 58; }
    private int cardGap() { return 8; }
    private int cardColumns() { return listWidth >= 330 ? 2 : 1; }
    private int visibleRows() { return Math.max(1, (contentBottom - listTop) / (cardHeight() + cardGap())); }
    private int maxScroll() {
        int rows = (visibleSpells.size() + cardColumns() - 1) / cardColumns();
        return Math.max(0, rows - visibleRows());
    }
    private int slotHeight() {
        return Math.max(30, Math.min(44,
                (contentBottom - slotsTop) / Math.max(1, CultivationClientState.spellSlotCount()) - 7));
    }
    private int[] slotRect(int slot) {
        int h = slotHeight();
        return new int[] {slotsLeft, slotsTop + slot * (h + 7), slotsWidth, h};
    }
    private int[] cardRect(int index) {
        int cols = cardColumns();
        int cardWidth = (listWidth - (cols - 1) * cardGap()) / cols;
        int row = index / cols - scroll;
        int col = index % cols;
        return new int[] {listLeft + col * (cardWidth + cardGap()),
                listTop + row * (cardHeight() + cardGap()), cardWidth, cardHeight()};
    }
    private boolean inside(double x, double y, int[] r) {
        return x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3];
    }
    private int slotAt(double x, double y) {
        for (int i = 0; i < CultivationClientState.spellSlotCount(); i++) {
            if (inside(x, y, slotRect(i))) return i;
        }
        return -1;
    }
    private int cardAt(double x, double y) {
        int rows = visibleRows();
        for (int i = 0; i < visibleSpells.size(); i++) {
            int row = i / cardColumns() - scroll;
            if (row >= 0 && row < rows && inside(x, y, cardRect(i))) return i;
        }
        return -1;
    }
    private void equip(int slot, int spellIndex) {
        if (slot < 0 || spellIndex < 0 || spellIndex >= visibleSpells.size()) return;
        selectedSlot = slot;
        selectedSpell = spellIndex;
        XiuxianNetwork.requestEquipSpell(slot, visibleSpells.get(spellIndex).id());
    }
    private void clearSlot(int slot) {
        if (slot >= 0) XiuxianNetwork.requestEquipSpell(slot, "");
    }
    private String slotLabel(int slot) {
        CultivationSpell spell = CultivationSpells.byId(CultivationClientState.spellAt(slot));
        return spell == null ? "\u7a7a\u69fd" : spell.displayName();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xA807100D);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 3, GOLD);
        graphics.fill(panelLeft, panelTop + panelHeight - 3, panelLeft + panelWidth, panelTop + panelHeight, GOLD);
        graphics.drawCenteredString(font, "\u7075\u53f0\u672f\u6cd5\u88c5\u914d", width / 2, panelTop + 13, GOLD);
        graphics.drawCenteredString(font,
                "\u5de6\u952e\u62d6\u62fd\u672f\u6cd5\u5230\u5feb\u6377\u69fd\uff0c\u53f3\u952e\u6e05\u7a7a\u69fd\u4f4d\uff0c\u6eda\u8f6e\u67e5\u770b\u5217\u8868",
                width / 2, panelTop + 31, MUTED);
        graphics.fill(slotsLeft - 8, panelTop + 60, slotsLeft + slotsWidth + 8, contentBottom, PANEL_DARK);
        graphics.fill(listLeft - 8, panelTop + 60, listLeft + listWidth + 8, contentBottom, PANEL_DARK);
        graphics.fill(detailLeft, detailTop, detailLeft + detailWidth, detailTop + detailHeight, PANEL_LIGHT);
        graphics.drawString(font, "\u5feb\u6377\u69fd\u4f4d  \u00b7  "
                + CultivationClientState.spellSlotCount() + " \u4e2a", slotsLeft, panelTop + 67, TEXT, false);
        graphics.drawString(font, "\u5907\u9009\u672f\u6cd5  \u00b7  "
                + visibleSpells.size() + " \u95e8", listLeft, panelTop + 67, TEXT, false);
        if (!compactLayout) graphics.drawString(font, "\u672f\u6cd5\u8be6\u60c5",
                detailLeft + 12, panelTop + 67, TEXT, false);
        graphics.drawString(font, "Esc \u8fd4\u56de", panelLeft + panelWidth - 68,
                panelTop + panelHeight - 12, MUTED, false);
        for (int slot = 0; slot < CultivationClientState.spellSlotCount(); slot++) {
            renderSlot(graphics, slot, mouseX, mouseY);
        }
        for (int i = 0; i < visibleSpells.size(); i++) {
            int[] r = cardRect(i);
            if (r[1] < listTop || r[1] + r[3] > contentBottom) continue;
            renderCard(graphics, i, r, mouseX, mouseY);
        }
        if (compactLayout) graphics.drawString(font, "\u672f\u6cd5\u8be6\u60c5",
                detailLeft + 12, detailTop + 6, TEXT, false);
        renderDetails(graphics);
        if (dragging && draggingSpell >= 0 && draggingSpell < visibleSpells.size()) {
            CultivationSpell spell = visibleSpells.get(draggingSpell);
            int w = Math.min(210, Math.max(120, font.width(spell.displayName()) + 38));
            graphics.fill((int) dragX - w / 2, (int) dragY - 18,
                    (int) dragX + w / 2, (int) dragY + 18, 0xF02A473B);
            graphics.renderOutline((int) dragX - w / 2, (int) dragY - 18, w, 36, GOLD);
            graphics.drawCenteredString(font, spell.displayName(), (int) dragX, (int) dragY - 4, TEXT);
            graphics.drawCenteredString(font, "\u677e\u5f00\u653e\u5165\u69fd\u4f4d", (int) dragX, (int) dragY + 9, MUTED);
        }
    }

    private void renderSlot(GuiGraphics graphics, int slot, int mouseX, int mouseY) {
        int[] r = slotRect(slot);
        boolean selected = selectedSlot == slot;
        boolean hovered = inside(mouseX, mouseY, r);
        int fill = selected ? 0xE08C6B3E : hovered ? 0xE0526A55 : 0xC41F2B26;
        graphics.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
        graphics.renderOutline(r[0], r[1], r[2], r[3], selected ? GOLD : 0xFF667A65);
        graphics.drawString(font, String.format("%02d", slot + 1), r[0] + 9,
                r[1] + (r[3] - 8) / 2, GOLD, false);
        graphics.drawString(font, slotLabel(slot), r[0] + 42,
                r[1] + (r[3] - 8) / 2, TEXT, false);
        if (selected) graphics.drawString(font, "\u5f53\u524d", r[0] + r[2] - 35,
                r[1] + (r[3] - 8) / 2, MUTED, false);
    }

    private void renderCard(GuiGraphics graphics, int index, int[] r, int mouseX, int mouseY) {
        CultivationSpell spell = visibleSpells.get(index);
        boolean hovered = inside(mouseX, mouseY, r);
        boolean selected = selectedSpell == index;
        int fill = selected ? 0xE08C6B3E : hovered ? 0xE0526A55 : 0xC42A3930;
        graphics.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
        graphics.renderOutline(r[0], r[1], r[2], r[3], selected ? GOLD : 0xFF667A65);
        graphics.drawString(font, spell.displayName(), r[0] + 10, r[1] + 8, TEXT, false);
        graphics.drawString(font, spell.element().displayName() + "  \u00b7  "
                + spell.trueQiCost() + " \u771f\u6c14", r[0] + 10, r[1] + 24, elementColor(spell), false);
        graphics.drawString(font, spell.usage(), r[0] + 10, r[1] + 40, MUTED, false);
    }

    private void renderDetails(GuiGraphics graphics) {
        CultivationSpell selected = selectedSpell >= 0 && selectedSpell < visibleSpells.size()
                ? visibleSpells.get(selectedSpell) : null;
        int x = detailLeft + 12;
        int y = detailTop + 16 - detailScroll;
        int sourceY = detailTop + detailHeight - 18;
        if (selected == null) {
            graphics.drawString(font, "\u9009\u62e9\u4e00\u95e8\u672f\u6cd5\u67e5\u770b\u8be6\u60c5", x, y, MUTED, false);
            graphics.drawString(font, "\u62d6\u62fd\u5230\u5de6\u4fa7\u69fd\u4f4d\u5373\u53ef\u88c5\u914d", x, y + 18, MUTED, false);
            return;
        }
        graphics.drawString(font, selected.displayName(), x, y, GOLD, false);
        graphics.drawString(font, "\u5c5e\u6027\uff1a" + selected.element().displayName(), x, y + 22, TEXT, false);
        graphics.drawString(font, "\u6d88\u8017\uff1a" + selected.trueQiCost() + " \u771f\u6c14", x, y + 39, TEXT, false);
        graphics.drawString(font, "\u51b7\u5374\uff1a" + Math.max(1, selected.cooldownTicks() / 20)
                + " \u79d2", x, y + 56, TEXT, false);
        graphics.drawString(font, "\u8303\u56f4\uff1a" + selected.range() + " \u7c73  \u00b7  "
                + selected.usage(), x, y + 73, TEXT, false);
        int lineY = y + 101;
        int maxLineY = sourceY - 12;
        for (FormattedCharSequence line : font.split(Component.literal(selected.description()),
                Math.max(100, detailWidth - 28))) {
            if (lineY > maxLineY) break;
            graphics.drawString(font, line, x, lineY, MUTED, false);
            lineY += 13;
        }
        graphics.drawString(font, CultivationSpells.isAutomaticallyLearned(selected.id())
                ? "\u6765\u6e90\uff1a\u5883\u754c\u81ea\u609f" : "\u6765\u6e90\uff1a\u672f\u6cd5\u4e66\u6216\u5947\u9047",
                x, sourceY, GREEN, false);
    }

    private int elementColor(CultivationSpell spell) {
        return switch (spell.element()) {
            case METAL -> 0xFFE5E8D2; case WOOD -> 0xFF8FD39A; case WATER -> 0xFF73C7E7;
            case FIRE -> 0xFFFF9A5C; case EARTH -> 0xFFD9B276; case WIND -> 0xFFB4E1C1;
            case THUNDER -> 0xFFE9E277; case SOUL -> 0xFFC9A6FF; default -> 0xFFB7C9BC;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            int slot = slotAt(mouseX, mouseY);
            if (slot >= 0) { selectedSlot = slot; clearSlot(slot); return true; }
        }
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int slot = slotAt(mouseX, mouseY);
        if (slot >= 0) { selectedSlot = slot; return true; }
        int card = cardAt(mouseX, mouseY);
        if (card >= 0) {
            selectedSpell = card;
            draggingSpell = card;
            dragging = true;
            dragX = mouseX;
            dragY = mouseY;
            detailScroll = 0;
            return true;
        }
        if (mouseX >= panelLeft + panelWidth - 92 && mouseY >= panelTop + panelHeight - 38) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragging) { this.dragX = mouseX; this.dragY = mouseY; return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            int slot = slotAt(mouseX, mouseY);
            if (slot >= 0) equip(slot, draggingSpell);
            dragging = false;
            draggingSpell = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listLeft - 8 && mouseX <= listLeft + listWidth + 8
                && mouseY >= listTop && mouseY <= contentBottom) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
            return true;
        }
        if (mouseX >= detailLeft && mouseX <= detailLeft + detailWidth
                && mouseY >= detailTop && mouseY <= detailTop + detailHeight) {
            detailScroll = Math.max(0, Math.min(52, detailScroll - (int) Math.signum(delta) * 13));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
