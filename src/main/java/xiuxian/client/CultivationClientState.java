package xiuxian.client;

import xiuxian.cultivation.CultivationPath;
import xiuxian.cultivation.CultivationRealm;
import xiuxian.cultivation.FamilyOrigin;
import xiuxian.cultivation.CultivationTechnique;
import xiuxian.cultivation.CultivationTechniques;

public final class CultivationClientState {
    private static boolean initialized;
    private static FamilyOrigin familyOrigin = FamilyOrigin.MORTAL;
    private static CultivationPath cultivationPath = CultivationPath.WANDERER;
    private static CultivationRealm realm = CultivationRealm.FETAL_BREATH;
    private static int realmLevel = 1;
    private static int qi;
    private static int breakthroughCost;
    private static String techniqueId = "xiuxian:basic_breathing";
    private static boolean meditating;
    private static int spiritualRoot = 10;
    private static int constitution = 10;
    private static int comprehension = 10;
    private static int fortune = 10;
    private static String studyingTechniqueId = "";
    private static int techniqueStudyTicks;
    private static int techniqueStudyDuration;
    private static int techniqueStudyChance;
    private static int trueQi;
    private static int trueQiMaximum;
    private static int alchemyLevel = 1;
    private static int alchemyExperience;
    private static int alchemyExperienceToNextLevel = 100;

    private CultivationClientState() {}

    public static boolean isInitialized() {
        return initialized;
    }

    public static FamilyOrigin familyOrigin() {
        return familyOrigin;
    }

    public static CultivationPath cultivationPath() {
        return cultivationPath;
    }

    public static CultivationRealm realm() {
        return realm;
    }

    public static int realmLevel() {
        return realmLevel;
    }

    public static int qi() {
        return qi;
    }

    public static int breakthroughCost() {
        return breakthroughCost;
    }

    public static int breakthroughChance() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return realm.breakthroughChanceAt(realmLevel, spiritualRoot, comprehension, fortune,
                technique == null ? 100 : technique.breakthroughCostPercent());
    }

    public static int passiveHealthRecoveryIntervalTicks() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return !initialized || technique == null ? 0 : realm.passiveHealthRecoveryIntervalTicksAt(
                realmLevel, constitution, technique.passiveHealthRecoveryPercent());
    }

    public static int passiveTrueQiRecoveryPerTenSeconds() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return !initialized || technique == null ? 0 : realm.passiveTrueQiRecoveryPerTenSecondsAt(
                realmLevel, comprehension, technique.trueQiRecoveryPerSecond());
    }

    public static String techniqueId() {
        return techniqueId;
    }

    public static boolean isMeditating() {
        return meditating;
    }

    public static int spiritualRoot() {
        return spiritualRoot;
    }

    public static int constitution() {
        return constitution;
    }

    public static int comprehension() {
        return comprehension;
    }

    public static int fortune() {
        return fortune;
    }

    public static String studyingTechniqueId() {
        return studyingTechniqueId;
    }

    public static int techniqueStudyTicks() {
        return techniqueStudyTicks;
    }

    public static int techniqueStudyDuration() {
        return techniqueStudyDuration;
    }

    public static int techniqueStudyChance() {
        return techniqueStudyChance;
    }

    public static int trueQi() { return trueQi; }
    public static int trueQiMaximum() { return trueQiMaximum; }
    public static int alchemyLevel() { return alchemyLevel; }
    public static int alchemyExperience() { return alchemyExperience; }
    public static int alchemyExperienceToNextLevel() { return alchemyExperienceToNextLevel; }

    public static void update(boolean newInitialized, String familyId, String pathId, String realmId,
                              int newRealmLevel, int newQi, int newBreakthroughCost,
                              String newTechniqueId, boolean newMeditating,
                              int newSpiritualRoot, int newConstitution,
                              int newComprehension, int newFortune, String newStudyingTechniqueId,
                              int newTechniqueStudyTicks, int newTechniqueStudyDuration,
                              int newTechniqueStudyChance, int newTrueQi, int newTrueQiMaximum,
                              int newAlchemyLevel, int newAlchemyExperience,
                              int newAlchemyExperienceToNextLevel) {
        initialized = newInitialized;
        familyOrigin = FamilyOrigin.byId(familyId);
        cultivationPath = CultivationPath.byId(pathId);
        realm = CultivationRealm.byId(realmId);
        realmLevel = Math.max(1, newRealmLevel);
        qi = Math.max(0, newQi);
        breakthroughCost = Math.max(0, newBreakthroughCost);
        techniqueId = newTechniqueId;
        meditating = newMeditating;
        spiritualRoot = clampAttribute(newSpiritualRoot);
        constitution = clampAttribute(newConstitution);
        comprehension = clampAttribute(newComprehension);
        fortune = clampAttribute(newFortune);
        studyingTechniqueId = newStudyingTechniqueId == null ? "" : newStudyingTechniqueId;
        techniqueStudyTicks = Math.max(0, newTechniqueStudyTicks);
        techniqueStudyDuration = Math.max(0, newTechniqueStudyDuration);
        techniqueStudyChance = Math.max(0, Math.min(100, newTechniqueStudyChance));
        trueQi = Math.max(0, newTrueQi);
        trueQiMaximum = Math.max(0, newTrueQiMaximum);
        alchemyLevel = Math.max(1, newAlchemyLevel);
        alchemyExperience = Math.max(0, newAlchemyExperience);
        alchemyExperienceToNextLevel = Math.max(1, newAlchemyExperienceToNextLevel);
    }

    private static int clampAttribute(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
