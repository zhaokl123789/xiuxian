package xiuxian.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;
import xiuxian.client.screen.CultivationProfileScreen;
import xiuxian.network.XiuxianNetwork;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CultivationKeyMappings {
    private static final KeyMapping MEDITATE = new KeyMapping("key.xiuxian.meditate", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.xiuxian");
    private static final KeyMapping BREAKTHROUGH = new KeyMapping("key.xiuxian.breakthrough", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.xiuxian");
    private static final KeyMapping PROFILE = new KeyMapping("key.xiuxian.profile", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.xiuxian");
    private static final KeyMapping VOID_WALK = new KeyMapping("key.xiuxian.void_walk", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.xiuxian");
    private static final KeyMapping SPELL_LOADOUT = new KeyMapping("key.xiuxian.spell_loadout", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.xiuxian");

    private CultivationKeyMappings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(MEDITATE);
        event.register(BREAKTHROUGH);
        event.register(PROFILE);
        event.register(VOID_WALK);
        event.register(SPELL_LOADOUT);
        MinecraftForge.EVENT_BUS.addListener(CultivationKeyMappings::onKeyInput);
    }

    private static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen != null) {
            discardPendingClicks();
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS && (CultivationClientState.isMeditating()
                || !CultivationClientState.studyingTechniqueId().isEmpty()) && isInterruptKey(minecraft, event)) {
            XiuxianNetwork.requestChannelInterrupt();
            return;
        }

        while (MEDITATE.consumeClick()) {
            minecraft.player.connection.sendCommand("xiuxian meditate");
        }
        while (BREAKTHROUGH.consumeClick()) {
            minecraft.player.connection.sendCommand("xiuxian breakthrough");
        }
        while (PROFILE.consumeClick()) {
            if (CultivationClientState.isInitialized()) {
                minecraft.setScreen(new CultivationProfileScreen(null));
            }
        }
        while (VOID_WALK.consumeClick()) {
            XiuxianNetwork.requestVoidWalk();
        }
        while (SPELL_LOADOUT.consumeClick()) {
            if (CultivationClientState.isInitialized()) {
                minecraft.setScreen(new xiuxian.client.screen.SpellLoadoutScreen());
            }
        }
    }

    private static boolean isInterruptKey(Minecraft minecraft, InputEvent.Key event) {
        int key = event.getKey();
        int scanCode = event.getScanCode();
        return minecraft.options.keyUp.matches(key, scanCode)
                || minecraft.options.keyDown.matches(key, scanCode)
                || minecraft.options.keyLeft.matches(key, scanCode)
                || minecraft.options.keyRight.matches(key, scanCode)
                || minecraft.options.keyJump.matches(key, scanCode);
    }

    private static void discardPendingClicks() {
        while (MEDITATE.consumeClick()) {}
        while (BREAKTHROUGH.consumeClick()) {}
        while (PROFILE.consumeClick()) {}
        while (VOID_WALK.consumeClick()) {}
        while (SPELL_LOADOUT.consumeClick()) {}
    }
}
