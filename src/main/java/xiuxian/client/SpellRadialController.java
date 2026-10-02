package xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import xiuxian.cultivation.CultivationSpell;
import xiuxian.cultivation.CultivationSpells;
import xiuxian.network.XiuxianNetwork;

/** Mouse-wheel radial selector: hold middle mouse to choose, release to cast; tap to cast current slot. */
@Mod.EventBusSubscriber(modid = "xiuxian", value = net.minecraftforge.api.distmarker.Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpellRadialController {
    private static final long HOLD_THRESHOLD_NANOS = 180_000_000L;
    private static boolean open;
    private static long pressedAt;
    private static int selectedSlot;

    private SpellRadialController() {}

    public static boolean isOpen() { return open; }
    public static int selectedSlot() { return selectedSlot; }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;
        if (event.getAction() == GLFW.GLFW_RELEASE && open) {
            boolean canCast = minecraft.player != null && minecraft.screen == null
                    && CultivationClientState.isInitialized();
            boolean held = System.nanoTime() - pressedAt >= HOLD_THRESHOLD_NANOS;
            if (canCast && held) updateSelection(minecraft);
            open = false;
            if (canCast && (held || !CultivationClientState.spellAt(selectedSlot).isBlank())) {
                XiuxianNetwork.requestCastSpell(CultivationClientState.spellAt(selectedSlot));
            }
            event.setCanceled(true);
            return;
        }
        if (minecraft.player == null || minecraft.screen != null || !CultivationClientState.isInitialized()) return;
        if (event.getAction() == GLFW.GLFW_PRESS) {
            pressedAt = System.nanoTime();
            open = true;
            updateSelection(minecraft);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!open) return;
        selectedSlot = Math.floorMod(selectedSlot - (int) Math.signum(event.getScrollDelta()), 4);
        event.setCanceled(true);
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
        selectedSlot = Math.floorMod((int) Math.floor((angle + Math.PI / 4.0D) / (Math.PI / 2.0D)), 4);
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (!open) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        updateSelection(minecraft);
        GuiGraphics graphics = event.getGuiGraphics();
        int cx = minecraft.getWindow().getGuiScaledWidth() / 2;
        int cy = minecraft.getWindow().getGuiScaledHeight() / 2;
        graphics.fill(cx - 132, cy - 132, cx + 132, cy + 132, 0x40101712);
        graphics.renderOutline(cx - 130, cy - 130, 260, 260, 0xAA9C7B42);
        graphics.renderOutline(cx - 102, cy - 102, 204, 204, 0x66566D5B);
        graphics.fill(cx - 36, cy - 36, cx + 36, cy + 36, 0xE016211D);
        graphics.renderOutline(cx - 36, cy - 36, 72, 72, 0xFFE0B968);
        graphics.drawCenteredString(minecraft.font, "\u7075", cx, cy - 5, 0xFFE8D8AF);
        int[][] positions = {{cx - 42, cy - 112}, {cx + 70, cy - 18}, {cx - 42, cy + 94}, {cx - 154, cy - 18}};
        for (int i = 0; i < 4; i++) {
            CultivationSpell spell = CultivationSpells.byId(CultivationClientState.spellAt(i));
            int accent = spell == null ? 0xFF667A65 : elementColor(spell);
            int color = i == selectedSlot ? (accent & 0x00FFFFFF) | 0xE0000000 : 0xB5202D28;
            graphics.fill(positions[i][0], positions[i][1], positions[i][0] + 84, positions[i][1] + 34, color);
            graphics.renderOutline(positions[i][0], positions[i][1], 84, 34, i == selectedSlot ? accent : 0xAA667A65);
            String name = spell == null ? "\u7a7a\u69fd" : spell.displayName();
            graphics.drawCenteredString(minecraft.font, name, positions[i][0] + 42, positions[i][1] + 7, 0xFFE5DDCA);
            graphics.drawCenteredString(minecraft.font, spell == null ? "" : spell.element().displayName(),
                    positions[i][0] + 42, positions[i][1] + 20, accent);
        }
        CultivationSpell selected = CultivationSpells.byId(CultivationClientState.spellAt(selectedSlot));
        if (selected != null) {
            graphics.drawCenteredString(minecraft.font,
                    Component.literal(selected.displayName() + "  " + selected.trueQiCost() + "\u771f\u6c14"),
                    cx, cy + 49, elementColor(selected));
        }
    }

    private static int elementColor(CultivationSpell spell) {
        return switch (spell.element()) {
            case METAL -> 0xFFE5E8D2;
            case WOOD -> 0xFF8FD39A;
            case WATER -> 0xFF73C7E7;
            case FIRE -> 0xFFFF9A5C;
            case EARTH -> 0xFFD9B276;
            case WIND -> 0xFFB4E1C1;
            case THUNDER -> 0xFFE9E277;
            case SOUL -> 0xFFC9A6FF;
            default -> 0xFFB7C9BC;
        };
    }
}
