package xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import xiuxian.cultivation.CultivationSpell;
import xiuxian.cultivation.CultivationSpells;
import xiuxian.network.XiuxianNetwork;

/** Middle mouse gesture: tap casts, hold opens the radial selector. */
@Mod.EventBusSubscriber(modid = "xiuxian", value = net.minecraftforge.api.distmarker.Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpellRadialController {
    private static final long HOLD_THRESHOLD_NANOS = 220_000_000L;
    private static boolean pendingPress;
    private static boolean open;
    private static boolean restoreMouseOnFinish;
    private static long pressedAt;
    private static int selectedSlot;

    private SpellRadialController() {}

    public static boolean isOpen() { return open; }
    public static int selectedSlot() { return selectedSlot; }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (pendingPress || open) {
                event.setCanceled(true);
                return;
            }
            if (!canUse(minecraft)) return;
            pendingPress = true;
            pressedAt = System.nanoTime();
            selectedSlot = clampSlot(selectedSlot);
            restoreMouseOnFinish = minecraft.mouseHandler.isMouseGrabbed();
            // Stop the vanilla middle-button action and prevent camera rotation during the gesture.
            minecraft.mouseHandler.releaseMouse();
            event.setCanceled(true);
            return;
        }
        if (event.getAction() != GLFW.GLFW_RELEASE || (!pendingPress && !open)) return;
        event.setCanceled(true);
        if (pendingPress) {
            boolean held = System.nanoTime() - pressedAt >= HOLD_THRESHOLD_NANOS;
            pendingPress = false;
            if (held) updateSelection(minecraft);
            castSelected(minecraft);
        } else {
            updateSelection(minecraft);
            open = false;
            castSelected(minecraft);
        }
        restoreMouse(minecraft);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pendingPress) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!canUse(minecraft)) {
            pendingPress = false;
            open = false;
            restoreMouse(minecraft);
            return;
        }
        if (System.nanoTime() - pressedAt < HOLD_THRESHOLD_NANOS) return;
        pendingPress = false;
        open = true;
        updateSelection(minecraft);
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!pendingPress && !open) return;
        if (open) {
            int slotCount = Math.max(1, CultivationClientState.spellSlotCount());
            selectedSlot = Math.floorMod(selectedSlot - (int) Math.signum(event.getScrollDelta()), slotCount);
        }
        event.setCanceled(true);
    }

    private static boolean canUse(Minecraft minecraft) {
        return minecraft.player != null && minecraft.screen == null
                && CultivationClientState.isInitialized()
                && CultivationClientState.spellSlotCount() > 0;
    }

    private static int clampSlot(int slot) {
        return Math.max(0, Math.min(Math.max(1, CultivationClientState.spellSlotCount()) - 1, slot));
    }

    private static void castSelected(Minecraft minecraft) {
        if (!canUse(minecraft)) return;
        String spellId = CultivationClientState.spellAt(clampSlot(selectedSlot));
        if (spellId != null && !spellId.isBlank()) XiuxianNetwork.requestCastSpell(spellId);
    }

    private static void restoreMouse(Minecraft minecraft) {
        if (restoreMouseOnFinish && minecraft.player != null && minecraft.screen == null) {
            minecraft.mouseHandler.grabMouse();
        }
        restoreMouseOnFinish = false;
    }

    private static void updateSelection(Minecraft minecraft) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        double mouseX = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
        double dx = mouseX - width / 2.0D;
        double dy = mouseY - height / 2.0D;
        if (dx * dx + dy * dy < 900.0D) return;
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;
        int slotCount = Math.max(1, CultivationClientState.spellSlotCount());
        selectedSlot = Math.floorMod((int) Math.floor((angle + Math.PI / slotCount)
                / (Math.PI * 2.0D / slotCount)), slotCount);
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (!open) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!canUse(minecraft)) return;
        updateSelection(minecraft);
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int cx = width / 2;
        int cy = height / 2 - 6;
        int slotCount = Math.max(1, CultivationClientState.spellSlotCount());

        // A quiet veil keeps the selector readable without freezing or moving
        // the world camera.  The selector itself stays in screen space.
        graphics.fill(0, 0, width, height, 0x7A06100D);
        int panel = Math.min(360, Math.min(width - 28, height - 42));
        int left = cx - panel / 2;
        int top = cy - panel / 2;
        graphics.fill(left, top, left + panel, top + panel, 0xB914211C);
        graphics.renderOutline(left, top, panel, panel, 0xD3C8A05A);
        graphics.renderOutline(left + 8, top + 8, panel - 16, panel - 16, 0x665B806A);
        graphics.renderOutline(left + 22, top + 22, panel - 44, panel - 44, 0x554D6D5C);
        graphics.drawCenteredString(minecraft.font, "\u7075\u8f6e  \u00b7  \u9009\u62e9\u672f\u6cd5", cx, top + 14, 0xFFE4C77E);

        int hubSize = 94;
        graphics.fill(cx - hubSize / 2, cy - hubSize / 2, cx + hubSize / 2, cy + hubSize / 2, 0xE0182B24);
        graphics.renderOutline(cx - hubSize / 2, cy - hubSize / 2, hubSize, hubSize, 0xFFE0B968);
        graphics.renderOutline(cx - hubSize / 2 + 6, cy - hubSize / 2 + 6, hubSize - 12, hubSize - 12, 0x667EAF8D);
        graphics.drawCenteredString(minecraft.font, "\u6309\u4f4f\u4e2d\u952e", cx, cy - 10, 0xFFE8E0CC);
        graphics.drawCenteredString(minecraft.font, "\u79fb\u52a8\u9f20\u6807\u9009\u62e9", cx, cy + 7, 0xFF9EB0A1);

        double radius = Math.min(132.0D, panel * 0.39D);
        double step = Math.PI * 2.0D / slotCount;
        int nodeWidth = slotCount >= 8 ? 92 : 112;
        int nodeHeight = 48;
        for (int i = 0; i < slotCount; i++) {
            double theta = -Math.PI / 2.0D + i * step;
            int nodeX = (int) Math.round(cx + Math.cos(theta) * radius) - nodeWidth / 2;
            int nodeY = (int) Math.round(cy + Math.sin(theta) * radius) - nodeHeight / 2;
            CultivationSpell spell = CultivationSpells.byId(CultivationClientState.spellAt(i));
            int accent = spell == null ? 0xFF667A65 : elementColor(spell);
            boolean selected = i == selectedSlot;
            int drawX = selected ? nodeX - 5 : nodeX;
            int drawY = selected ? nodeY - 5 : nodeY;
            int drawW = selected ? nodeWidth + 10 : nodeWidth;
            int drawH = selected ? nodeHeight + 10 : nodeHeight;
            graphics.fill(drawX, drawY, drawX + drawW, drawY + drawH,
                    selected ? ((accent & 0x00FFFFFF) | 0xE015211C) : 0xD21A2923);
            graphics.renderOutline(drawX, drawY, drawW, drawH, selected ? accent : 0xA067806E);
            if (selected) graphics.renderOutline(drawX - 3, drawY - 3, drawW + 6, drawH + 6, 0x557ED09D);
            String name = spell == null ? "\u7a7a\u69fd" : spell.displayName();
            graphics.drawCenteredString(minecraft.font, String.format("%02d  %s", i + 1, name),
                    drawX + drawW / 2, drawY + 9, 0xFFE8E0CC);
            graphics.drawCenteredString(minecraft.font, spell == null ? "\u672a\u88c5\u914d" :
                    spell.element().displayName() + "  \u00b7  " + spell.trueQiCost() + " \u771f\u6c14",
                    drawX + drawW / 2, drawY + 26, spell == null ? 0xFF829187 : accent);
        }
        CultivationSpell selected = CultivationSpells.byId(CultivationClientState.spellAt(selectedSlot));
        if (selected != null) {
            graphics.drawCenteredString(minecraft.font, selected.displayName(), cx, top + panel - 34, elementColor(selected));
        }
        graphics.drawCenteredString(minecraft.font, "\u77ed\u6309\u4e2d\u952e\u65bd\u6cd5  \u00b7  \u957f\u6309\u8fdb\u5165\u7075\u8f6e  \u00b7  \u6eda\u8f6e\u53ef\u5207\u6362", cx, top + panel - 18, 0xFF9EB0A1);
    }

    private static int elementColor(CultivationSpell spell) {
        return switch (spell.element()) {
            case METAL -> 0xFFE5E8D2; case WOOD -> 0xFF8FD39A; case WATER -> 0xFF73C7E7;
            case FIRE -> 0xFFFF9A5C; case EARTH -> 0xFFD9B276; case WIND -> 0xFFB4E1C1;
            case THUNDER -> 0xFFE9E277; case SOUL -> 0xFFC9A6FF; default -> 0xFFB7C9BC;
        };
    }
}
