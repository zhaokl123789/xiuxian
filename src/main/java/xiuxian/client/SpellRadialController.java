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
        graphics.fill(cx - 42, cy - 42, cx + 42, cy + 42, 0xD916211D);
        graphics.drawCenteredString(minecraft.font, "\u8f6e\u76d8\u65bd\u6cd5", cx, cy - 5, 0xFFE8D8AF);
        int[][] positions = {{cx - 34, cy - 86}, {cx + 46, cy - 10}, {cx - 34, cy + 66}, {cx - 114, cy - 10}};
        for (int i = 0; i < 4; i++) {
            int color = i == selectedSlot ? 0xE08C6B3E : 0xB52A3A31;
            graphics.fill(positions[i][0], positions[i][1], positions[i][0] + 68, positions[i][1] + 24, color);
            CultivationSpell spell = CultivationSpells.byId(CultivationClientState.spellAt(i));
            String name = spell == null ? "\u7a7a\u69fd" : spell.displayName();
            graphics.drawCenteredString(minecraft.font, name, positions[i][0] + 34, positions[i][1] + 7, 0xFFE5DDCA);
        }
        CultivationSpell selected = CultivationSpells.byId(CultivationClientState.spellAt(selectedSlot));
        if (selected != null) {
            graphics.drawCenteredString(minecraft.font,
                    Component.literal(selected.displayName() + "  " + selected.trueQiCost() + "\u771f\u6c14"),
                    cx, cy + 50, 0xFFB7C9BC);
        }
    }
}
