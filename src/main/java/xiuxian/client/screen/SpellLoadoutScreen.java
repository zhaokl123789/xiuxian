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

/** Drag-and-drop spell board. Rendering is kept in one surface so controls cannot overlap. */
public final class SpellLoadoutScreen extends Screen {
    private static final int PANEL = 0xF21A2420;
    private static final int PANEL_DARK = 0xD915201C;
    private static final int PANEL_LIGHT = 0xD92A3930;
    private static final int GOLD = 0xFFE0B968;
    private static final int TEXT = 0xFFE8E0CC;
    private static final int MUTED = 0xFF9EB0A1;
    private static final int GREEN = 0xFF8FCDA4;

    private int panelLeft, panelTop, panelWidth, panelHeight;
    private int slotsLeft, slotsTop, slotsWidth, listLeft, listTop, listWidth, detailLeft, detailWidth;
    private int scroll, selectedSlot, selectedSpell = -1, draggingSpell = -1;
    private double dragX, dragY;
    private boolean dragging;
    private List<CultivationSpell> visibleSpells = List.of();

    public SpellLoadoutScreen() { super(Component.literal("术法装配")); }

    @Override
    protected void init() {
        panelWidth = Math.min(1120, Math.max(300, width - 24));
        panelHeight = Math.min(650, Math.max(260, height - 24));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        slotsLeft = panelLeft + 18;
        slotsTop = panelTop + 83;
        slotsWidth = Math.min(280, Math.max(170, panelWidth / 4));
        listLeft = slotsLeft + slotsWidth + 16;
        listTop = slotsTop;
        listWidth = Math.min(430, Math.max(230, panelWidth / 3));
        detailLeft = listLeft + listWidth + 16;
        detailWidth = Math.max(170, panelLeft + panelWidth - detailLeft - 18);
        visibleSpells = availableSpells();
        scroll = Math.min(scroll, maxScroll());
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
    private int visibleRows() { return Math.max(1, (panelHeight - 113) / (cardHeight() + cardGap())); }
    private int maxScroll() {
        int rows = (visibleSpells.size() + cardColumns() - 1) / cardColumns();
        return Math.max(0, rows - visibleRows());
    }
    private int slotHeight() { return Math.max(30, Math.min(44, (panelHeight - 145) / Math.max(1, CultivationClientState.spellSlotCount()) - 7)); }
    private int[] slotRect(int slot) {
        int h = slotHeight();
        return new int[] {slotsLeft, slotsTop + slot * (h + 7), slotsWidth, h};
    }
    private int[] cardRect(int index) {
        int cols = cardColumns();
        int cardWidth = (listWidth - (cols - 1) * cardGap()) / cols;
        int row = index / cols - scroll;
        int col = index % cols;
        return new int[] {listLeft + col * (cardWidth + cardGap()), listTop + row * (cardHeight() + cardGap()), cardWidth, cardHeight()};
    }
    private boolean inside(double x, double y, int[] r) { return x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3]; }
    private int slotAt(double x, double y) {
        for (int i = 0; i < CultivationClientState.spellSlotCount(); i++) if (inside(x, y, slotRect(i))) return i;
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
    private void clearSlot(int slot) { if (slot >= 0) XiuxianNetwork.requestEquipSpell(slot, ""); }
    private String slotLabel(int slot) {
        CultivationSpell spell = CultivationSpells.byId(CultivationClientState.spellAt(slot));
        return spell == null ? "空槽" : spell.displayName();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xA807100D);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 3, GOLD);
        graphics.fill(panelLeft, panelTop + panelHeight - 3, panelLeft + panelWidth, panelTop + panelHeight, GOLD);
        graphics.drawCenteredString(font, "太虚术法谱", width / 2, panelTop + 13, GOLD);
        graphics.drawCenteredString(font, "按住术法卡拖入槽位；右键槽位清空；滚轮浏览备选术法", width / 2, panelTop + 31, MUTED);
        int contentBottom = panelTop + panelHeight - 18;
        graphics.fill(slotsLeft - 8, panelTop + 60, slotsLeft + slotsWidth + 8, contentBottom, PANEL_DARK);
        graphics.fill(listLeft - 8, panelTop + 60, listLeft + listWidth + 8, contentBottom, PANEL_DARK);
        graphics.fill(detailLeft, panelTop + 60, detailLeft + detailWidth, contentBottom, PANEL_LIGHT);
        graphics.drawString(font, "快捷槽位  ·  " + CultivationClientState.spellSlotCount() + " 格", slotsLeft, panelTop + 67, TEXT, false);
        graphics.drawString(font, "备选术法  ·  " + visibleSpells.size() + " 门", listLeft, panelTop + 67, TEXT, false);
        graphics.drawString(font, "术法详情", detailLeft + 12, panelTop + 67, TEXT, false);
        graphics.drawString(font, "Esc 返回", panelLeft + panelWidth - 68, panelTop + panelHeight - 12, MUTED, false);
        for (int slot = 0; slot < CultivationClientState.spellSlotCount(); slot++) renderSlot(graphics, slot, mouseX, mouseY);
        for (int i = 0; i < visibleSpells.size(); i++) {
            int[] r = cardRect(i);
            if (r[1] < listTop || r[1] + r[3] > contentBottom) continue;
            renderCard(graphics, i, r, mouseX, mouseY);
        }
        renderDetails(graphics);
        if (dragging && draggingSpell >= 0 && draggingSpell < visibleSpells.size()) {
            CultivationSpell spell = visibleSpells.get(draggingSpell);
            int w = Math.min(210, Math.max(120, font.width(spell.displayName()) + 38));
            graphics.fill((int) dragX - w / 2, (int) dragY - 18, (int) dragX + w / 2, (int) dragY + 18, 0xF02A473B);
            graphics.renderOutline((int) dragX - w / 2, (int) dragY - 18, w, 36, GOLD);
            graphics.drawCenteredString(font, spell.displayName(), (int) dragX, (int) dragY - 4, TEXT);
            graphics.drawCenteredString(font, "拖入槽位", (int) dragX, (int) dragY + 9, MUTED);
        }
    }

    private void renderSlot(GuiGraphics graphics, int slot, int mouseX, int mouseY) {
        int[] r = slotRect(slot);
        boolean selected = selectedSlot == slot;
        boolean hovered = inside(mouseX, mouseY, r);
        int fill = selected ? 0xE08C6B3E : hovered ? 0xE0526A55 : 0xC41F2B26;
        graphics.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
        graphics.renderOutline(r[0], r[1], r[2], r[3], selected ? GOLD : 0xFF667A65);
        graphics.drawString(font, String.format("%02d", slot + 1), r[0] + 9, r[1] + (r[3] - 8) / 2, GOLD, false);
        graphics.drawString(font, slotLabel(slot), r[0] + 42, r[1] + (r[3] - 8) / 2, TEXT, false);
        if (selected) graphics.drawString(font, "当前", r[0] + r[2] - 35, r[1] + (r[3] - 8) / 2, MUTED, false);
    }

    private void renderCard(GuiGraphics graphics, int index, int[] r, int mouseX, int mouseY) {
        CultivationSpell spell = visibleSpells.get(index);
        boolean hovered = inside(mouseX, mouseY, r);
        boolean selected = selectedSpell == index;
        int fill = selected ? 0xE08C6B3E : hovered ? 0xE0526A55 : 0xC42A3930;
        graphics.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
        graphics.renderOutline(r[0], r[1], r[2], r[3], selected ? GOLD : 0xFF667A65);
        graphics.drawString(font, spell.displayName(), r[0] + 10, r[1] + 8, TEXT, false);
        graphics.drawString(font, spell.element().displayName() + "  ·  " + spell.trueQiCost() + " 真气", r[0] + 10, r[1] + 24, elementColor(spell), false);
        graphics.drawString(font, spell.usage(), r[0] + 10, r[1] + 40, MUTED, false);
    }

    private void renderDetails(GuiGraphics graphics) {
        CultivationSpell selected = selectedSpell >= 0 && selectedSpell < visibleSpells.size() ? visibleSpells.get(selectedSpell) : null;
        int x = detailLeft + 12;
        int y = panelTop + 91;
        if (selected == null) {
            graphics.drawString(font, "拖拽一门术法到左侧槽位", x, y, MUTED, false);
            graphics.drawString(font, "选中后可在此查看施法参数", x, y + 18, MUTED, false);
            return;
        }
        graphics.drawString(font, selected.displayName(), x, y, GOLD, false);
        graphics.drawString(font, "属性：" + selected.element().displayName(), x, y + 22, TEXT, false);
        graphics.drawString(font, "消耗：" + selected.trueQiCost() + " 真气", x, y + 39, TEXT, false);
        graphics.drawString(font, "冷却：" + Math.max(1, selected.cooldownTicks() / 20) + " 秒", x, y + 56, TEXT, false);
        graphics.drawString(font, "范围：" + selected.range() + " 格  ·  " + selected.usage(), x, y + 73, TEXT, false);
        int lineY = y + 101;
        for (FormattedCharSequence line : font.split(Component.literal(selected.description()), Math.max(100, detailWidth - 28))) {
            graphics.drawString(font, line, x, lineY, MUTED, false);
            lineY += 13;
        }
        graphics.drawString(font, CultivationSpells.isAutomaticallyLearned(selected.id()) ? "来源：入境自悟" : "来源：奇遇术法书", x, panelTop + panelHeight - 38, GREEN, false);
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
            selectedSpell = card; draggingSpell = card; dragging = true; dragX = mouseX; dragY = mouseY; return true;
        }
        if (mouseX >= panelLeft + panelWidth - 92 && mouseY >= panelTop + panelHeight - 38) { onClose(); return true; }
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
            dragging = false; draggingSpell = -1; return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listLeft - 8 && mouseX <= listLeft + listWidth + 8) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta))); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
