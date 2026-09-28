package xiuxian.client;

import xiuxian.cultivation.CultivationPath;
import xiuxian.cultivation.CultivationRealm;
import xiuxian.cultivation.FamilyOrigin;

public final class CultivationClientState {
    private static boolean initialized;
    private static FamilyOrigin familyOrigin = FamilyOrigin.MORTAL;
    private static CultivationPath cultivationPath = CultivationPath.WANDERER;
    private static CultivationRealm realm = CultivationRealm.QI_REFINING;
    private static int realmLevel = 1;
    private static int qi;
    private static int breakthroughCost;
    private static String techniqueId = "xiuxian:basic_breathing";
    private static boolean meditating;
    private static int spiritualRoot = 50;
    private static int constitution = 50;
    private static int comprehension = 50;
    private static int fortune = 50;

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

    public static void update(boolean newInitialized, String familyId, String pathId, String realmId,
                              int newRealmLevel, int newQi, int newBreakthroughCost,
                              String newTechniqueId, boolean newMeditating,
                              int newSpiritualRoot, int newConstitution,
                              int newComprehension, int newFortune) {
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
    }

    private static int clampAttribute(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
