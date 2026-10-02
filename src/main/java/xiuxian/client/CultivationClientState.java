package xiuxian.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private static String immortalFoundation = "";
    private static int majorBreakthroughFailures;
    private static List<String> spellLoadout = List.of("", "", "", "");
    private static Set<String> learnedSpellIds = Set.of();

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
                technique == null ? 100 : technique.breakthroughCostPercent(),
                technique == null ? 0 : technique.breakthroughChanceBonus(), majorBreakthroughFailures);
    }

    public static int passiveHealthRecoveryIntervalTicks() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (!initialized) return 0;
        int recoveryPercent = technique == null ? 100 : technique.passiveHealthRecoveryPercent();
        if (technique != null && technique.elementalAffinity().equals("木")) {
            recoveryPercent = Math.min(200, recoveryPercent + 25);
        }
        return realm.passiveHealthRecoveryIntervalTicksAt(realmLevel, constitution, recoveryPercent);
    }

    public static int passiveTrueQiRecoveryPerTenSeconds() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (!initialized || technique == null) return 0;
        int recovery = realm.passiveTrueQiRecoveryPerTenSecondsAt(
                realmLevel, comprehension, technique.trueQiRecoveryPerSecond());
        return technique.elementalAffinity().equals("水") && recovery > 0
                ? recovery + Math.max(1, recovery / 3) : recovery;
    }

    public static int meditationQiPerSecondMilli() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return !initialized || technique == null ? 0 : technique.effectiveMeditationQiPerSecondMilli(
                spiritualRoot, constitution, comprehension, fortune, realm, realmLevel);
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

    public static void spendTrueQiLocally(int amount) {
        trueQi = Math.max(0, trueQi - Math.max(0, amount));
    }

    public static void updateTrueQi(int value) {
        trueQi = Math.max(0, Math.min(trueQiMaximum, value));
    }
    public static int alchemyLevel() { return alchemyLevel; }
    public static int alchemyExperience() { return alchemyExperience; }
    public static int alchemyExperienceToNextLevel() { return alchemyExperienceToNextLevel; }
    public static String immortalFoundation() { return immortalFoundation; }
    public static int majorBreakthroughFailures() { return majorBreakthroughFailures; }
    public static List<String> spellLoadout() { return Collections.unmodifiableList(spellLoadout); }
    public static String spellAt(int slot) {
        return slot >= 0 && slot < spellLoadout.size() ? spellLoadout.get(slot) : "";
    }
    public static Set<String> learnedSpellIds() { return learnedSpellIds; }
    public static int spellSlotCount() { return realm.spellSlotCount(); }

    public static void update(boolean newInitialized, String familyId, String pathId, String realmId,
                              int newRealmLevel, int newQi, int newBreakthroughCost,
                              String newTechniqueId, boolean newMeditating,
                              int newSpiritualRoot, int newConstitution,
                              int newComprehension, int newFortune, String newStudyingTechniqueId,
                              int newTechniqueStudyTicks, int newTechniqueStudyDuration,
                              int newTechniqueStudyChance, int newTrueQi, int newTrueQiMaximum,
                              int newAlchemyLevel, int newAlchemyExperience,
                              int newAlchemyExperienceToNextLevel, String newImmortalFoundation,
                              int newMajorBreakthroughFailures, List<String> newSpellLoadout,
                              Set<String> newLearnedSpellIds) {
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
        immortalFoundation = newImmortalFoundation == null ? "" : newImmortalFoundation;
        majorBreakthroughFailures = Math.max(0, newMajorBreakthroughFailures);
        List<String> slots = new ArrayList<>();
        if (newSpellLoadout != null) slots.addAll(newSpellLoadout);
        int slotCount = realm.spellSlotCount();
        while (slots.size() < slotCount) slots.add("");
        spellLoadout = List.copyOf(slots.subList(0, slotCount));
        learnedSpellIds = newLearnedSpellIds == null ? Set.of() : Set.copyOf(newLearnedSpellIds);
    }

    private static int clampAttribute(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
