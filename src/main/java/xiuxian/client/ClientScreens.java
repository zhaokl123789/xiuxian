package xiuxian.client;

import net.minecraft.client.Minecraft;
import xiuxian.client.screen.IdentityCreationScreen;

public final class ClientScreens {
    private ClientScreens() {}

    public static void openIdentityScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof IdentityCreationScreen)) {
            minecraft.setScreen(new IdentityCreationScreen());
        }
    }

    public static void closeIdentityScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof IdentityCreationScreen) {
            minecraft.setScreen(null);
        }
    }

    public static void updateCultivation(boolean initialized, String familyId, String pathId, String realmId,
                                         int realmLevel, int qi, int breakthroughCost,
                                         String techniqueId, boolean meditating,
                                         int spiritualRoot, int constitution, int comprehension, int fortune) {
        CultivationClientState.update(initialized, familyId, pathId, realmId, realmLevel, qi,
                breakthroughCost, techniqueId, meditating, spiritualRoot, constitution, comprehension, fortune);
    }
}
