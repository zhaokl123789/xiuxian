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
        int cx = minecraft.getWindow().getGuiScaledWidth() / 2;
        int cy = minecraft.getWindow().getGuiScaledHeight() / 2;
        int slotCount = Math.max(1, CultivationClientState.spellSlotCount());
        graphics.fill(cx - 154, cy - 154, cx + 154, cy + 154, 0x50101712);
        graphics.renderOutline(cx - 152, cy - 152, 304, 304, 0xAA9C7B42);
        graphics.renderOutline(cx - 138, cy - 138, 276, 276, 0x66566D5B);
        graphics.renderOutline(cx - 116, cy - 116, 232, 232, 0x66566D5B);
        graphics.fill(cx - 40, cy - 40, cx + 40, cy + 40, 0xE016211D);
        graphics.renderOutline(cx - 40, cy - 40, 80, 80, 0xFFE0B968);
        graphics.renderOutline(cx - 34, cy - 34, 68, 68, 0x886D9D7A);
        graphics.drawCenteredString(minecraft.font, "\u7075\u8f6e", cx, cy - 5, 0xFFE8D8AF);
        graphics.drawCenteredString(minecraft.font, "\u62d6\u66f3\u9009\u6cd5", cx, cy + 10, 0xFF9EB0A1);
        double radius = 118.0D;
        double step = Math.PI * 2.0D / slotCount;
        int nodeWidth = slotCount >= 8 ? 74 : 86;
        int nodeHeight = 38;
        for (int i = 0; i < slotCount; i++) {
            double theta = -Math.PI / 2.0D + i * step;
            int nodeX = (int) Math.round(cx + Math.cos(theta) * radius) - nodeWidth / 2;
            int nodeY = (int) Math.round(cy + Math.sin(theta) * radius) - nodeHeight / 2;
            CultivationSpell spell = CultivationSpells.byId(CultivationClientState.spellAt(i));
            int accent = spell == null ? 0xFF667A65 : elementColor(spell);
            int color = i == selectedSlot ? (accent & 0x00FFFFFF) | 0xE0000000 : 0xC5202D28;
            int drawX = i == selectedSlot ? nodeX - 3 : nodeX;
            int drawY = i == selectedSlot ? nodeY - 3 : nodeY;
            int drawW = i == selectedSlot ? nodeWidth + 6 : nodeWidth;
            int drawH = i == selectedSlot ? nodeHeight + 6 : nodeHeight;
            graphics.fill(drawX, drawY, drawX + drawW, drawY + drawH, color);
            graphics.renderOutline(drawX, drawY, drawW, drawH, i == selectedSlot ? accent : 0xAA667A65);
            String name = spell == null ? "\u7a7a\u69fd" : spell.displayName();
            graphics.drawCenteredString(minecraft.font, String.format("%02d  %s", i + 1, name),
                    drawX + drawW / 2, drawY + 7, 0xFFE5DDCA);
            graphics.drawCenteredString(minecraft.font, spell == null ? "" : spell.element().displayName(),
                    drawX + drawW / 2, drawY + 22, accent);
        }
        CultivationSpell selected = CultivationSpells.byId(CultivationClientState.spellAt(selectedSlot));
        if (selected != null) {
            graphics.drawCenteredString(minecraft.font,
                    selected.displayName() + "  " + selected.trueQiCost() + "\u771f\u6c14",
                    cx, cy + 49, elementColor(selected));
        }
        graphics.drawCenteredString(minecraft.font,
                "\u957f\u6309\u4e2d\u952e\u9009\u62e9  \u00b7  \u677e\u5f00\u65bd\u6cd5  \u00b7  \u6eda\u8f6e\u5207\u6362",
                cx, cy + 67, 0xFF9EB0A1);
    }

    private static int elementColor(CultivationSpell spell) {
        return switch (spell.element()) {
            case METAL -> 0xFFE5E8D2; case WOOD -> 0xFF8FD39A; case WATER -> 0xFF73C7E7;
            case FIRE -> 0xFFFF9A5C; case EARTH -> 0xFFD9B276; case WIND -> 0xFFB4E1C1;
            case THUNDER -> 0xFFE9E277; case SOUL -> 0xFFC9A6FF; default -> 0xFFB7C9BC;
        };
    }
}
