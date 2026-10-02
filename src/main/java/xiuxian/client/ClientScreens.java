package xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.List;
import java.util.Set;
import xiuxian.cultivation.CultivationTechnique;
import xiuxian.cultivation.CultivationTechniques;
import xiuxian.cultivation.CultivationRealm;
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
                                         String immortalFoundation, int majorBreakthroughFailures,
                                         List<String> spellLoadout, List<String> learnedSpellIds) {
        CultivationClientState.update(initialized, familyId, pathId, realmId, realmLevel, qi,
                breakthroughCost, techniqueId, meditating, spiritualRoot, constitution, comprehension, fortune,
                studyingTechniqueId, techniqueStudyTicks, techniqueStudyDuration, techniqueStudyChance,
                trueQi, trueQiMaximum, alchemyLevel, alchemyExperience, alchemyExperienceToNextLevel,
                immortalFoundation, majorBreakthroughFailures, spellLoadout,
                learnedSpellIds == null ? Set.of() : Set.copyOf(learnedSpellIds));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && initialized) {
            CultivationRealm realm = CultivationClientState.realm();
            CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
            double expectedMaxHealth = 20.0D + realm.healthBonusAt(CultivationClientState.realmLevel())
                    + (technique == null ? 0.0D : technique.healthBonus());
            AttributeInstance health = minecraft.player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(expectedMaxHealth);
                minecraft.player.setHealth(Math.min(minecraft.player.getHealth(), (float) expectedMaxHealth));
            }
        }
    }

    public static void openTechniqueBookScreen(String techniqueId) {
        Minecraft.getInstance().setScreen(new TechniqueBookScreen(techniqueId));
    }

    public static void openSpellLoadoutScreen() {
        Minecraft.getInstance().setScreen(new xiuxian.client.screen.SpellLoadoutScreen());
    }
}
