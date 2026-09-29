package xiuxian.client;

import net.minecraft.client.Minecraft;
import xiuxian.client.screen.IdentityCreationScreen;
import xiuxian.client.screen.TechniqueBookScreen;

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
                                         int spiritualRoot, int constitution, int comprehension, int fortune,
                                         String studyingTechniqueId, int techniqueStudyTicks,
                                         int techniqueStudyDuration, int techniqueStudyChance,
                                         int trueQi, int trueQiMaximum, int alchemyLevel,
                                         int alchemyExperience, int alchemyExperienceToNextLevel,
                                         String immortalFoundation, int majorBreakthroughFailures) {
        CultivationClientState.update(initialized, familyId, pathId, realmId, realmLevel, qi,
                breakthroughCost, techniqueId, meditating, spiritualRoot, constitution, comprehension, fortune,
                studyingTechniqueId, techniqueStudyTicks, techniqueStudyDuration, techniqueStudyChance,
                trueQi, trueQiMaximum, alchemyLevel, alchemyExperience, alchemyExperienceToNextLevel,
                immortalFoundation, majorBreakthroughFailures);
    }

    public static void openTechniqueBookScreen(String techniqueId) {
        Minecraft.getInstance().setScreen(new TechniqueBookScreen(techniqueId));
    }
}
