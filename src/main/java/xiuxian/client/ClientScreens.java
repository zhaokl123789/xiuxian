package xiuxian.client;

import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.client.screen.IdentityCreationScreen;
import xiuxian.client.screen.TechniqueBookScreen;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientScreens {
    private static boolean identitySelectionPending;

    private ClientScreens() {}

    public static void openIdentityScreen() {
        identitySelectionPending = true;
        showPendingIdentityScreen();
    }

    public static void closeIdentityScreen() {
        identitySelectionPending = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof IdentityCreationScreen) {
            minecraft.setScreen(null);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) showPendingIdentityScreen();
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        resetSession();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        resetSession();
    }

    private static void resetSession() {
        identitySelectionPending = false;
        CultivationClientState.reset();
    }

    private static void showPendingIdentityScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        // World loading may replace a screen after the login packets arrive.
        // Keep the request until vanilla has finished and released its screen.
        if (identitySelectionPending && minecraft.player != null && minecraft.player.isAlive()
                && minecraft.level != null
                && minecraft.screen == null && minecraft.getOverlay() == null) {
            minecraft.setScreen(new IdentityCreationScreen());
        }
    }

    public static void updateCultivation(boolean initialized, String familyId, String pathId, String realmId,
                                         int realmLevel, int qi, int breakthroughCost,
                                         String techniqueId, boolean meditating,
                                         int spiritualRoot, int constitution, int comprehension, int fortune,
                                         String studyingTechniqueId, int techniqueStudyTicks,
                                         int techniqueStudyDuration, int techniqueStudyChance,
                                         int trueQi, int trueQiMaximum, int alchemyLevel,
                                         int alchemyExperience, int alchemyExperienceToNextLevel,
                                         String immortalFoundation, int majorBreakthroughFailures,
                                         List<String> spellLoadout, List<String> learnedSpellIds) {
        CultivationClientState.update(initialized, familyId, pathId, realmId, realmLevel, qi,
                breakthroughCost, techniqueId, meditating, spiritualRoot, constitution, comprehension, fortune,
                studyingTechniqueId, techniqueStudyTicks, techniqueStudyDuration, techniqueStudyChance,
                trueQi, trueQiMaximum, alchemyLevel, alchemyExperience, alchemyExperienceToNextLevel,
                immortalFoundation, majorBreakthroughFailures, spellLoadout,
                learnedSpellIds == null ? Set.of() : Set.copyOf(learnedSpellIds));
        if (initialized) closeIdentityScreen();
        else openIdentityScreen();
    }

    public static void openTechniqueBookScreen(String techniqueId) {
        Minecraft.getInstance().setScreen(new TechniqueBookScreen(techniqueId));
    }

    public static void openSpellLoadoutScreen() {
        Minecraft.getInstance().setScreen(new xiuxian.client.screen.SpellLoadoutScreen());
    }
}
