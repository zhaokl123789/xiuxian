package xiuxian.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Set;

public class CultivationData implements INBTSerializable<CompoundTag> {
    public static final int DATA_VERSION = 9;
    public static final String STARTING_TECHNIQUE = "xiuxian:basic_breathing";
    public static final int BASE_ATTRIBUTE_MIN = 1;
    public static final int BASE_ATTRIBUTE_MAX = 12;
    public static final int MAX_ATTRIBUTE_SCORE = 100;

    private boolean initialized;
    private FamilyOrigin familyOrigin = FamilyOrigin.MORTAL;
    private CultivationPath cultivationPath = CultivationPath.WANDERER;
    private String techniqueId = STARTING_TECHNIQUE;
    private final Set<String> learnedTechniqueIds = new LinkedHashSet<>();
    private CultivationRealm realm = CultivationRealm.FETAL_BREATH;
    private int realmLevel = 1;
    private int qi;
    private int trueQi;
    private int alchemyLevel = 1;
    private int alchemyExperience;
    private int spiritualRoot = BASE_ATTRIBUTE_MIN;
    private int constitution = BASE_ATTRIBUTE_MIN;
    private int comprehension = BASE_ATTRIBUTE_MIN;
    private int fortune = BASE_ATTRIBUTE_MIN;
    private String immortalFoundation = "";
    private int majorBreakthroughFailures;
    private boolean meditating;
    private double meditationAnchorX;
    private double meditationAnchorY;
    private double meditationAnchorZ;
    private boolean wasCrouchingBeforeMeditation;
    private int stillTicks;
    private int meditationQiRemainder;
    private String studyingTechniqueId = "";
    private int techniqueStudyTicks;
    private int techniqueStudyDuration;
    private int techniqueStudyChance;
    private int techniqueStudyRoll;
    private int lastJumpBoostTick = -1000;
    private int lastObservedFoodLevel = -1;
    private boolean trueQiHealthRecovery;
    private boolean hasTaixuAnchor;
    private String taixuOriginDimension = "";
    private double taixuOriginX;
    private double taixuOriginY;
    private double taixuOriginZ;
    private float taixuOriginYaw;
    private float taixuOriginPitch;
    private float taixuOriginFlyingSpeed = 0.05F;
    private boolean taixuOriginMayfly;
    private boolean taixuOriginFlying;

    public boolean isInitialized() {
        return initialized;
    }

    public FamilyOrigin familyOrigin() {
        return familyOrigin;
    }

    public CultivationPath cultivationPath() {
        return cultivationPath;
    }

    public String techniqueId() {
        return techniqueId;
    }

    public Set<String> learnedTechniqueIds() {
        return Collections.unmodifiableSet(learnedTechniqueIds);
    }

    public CultivationRealm realm() {
        return realm;
    }

    public int realmLevel() {
        return realmLevel;
    }

    public int qi() {
        return qi;
    }

    public int trueQi() { return trueQi; }

    public int trueQiMaximum() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        int bonus = technique == null ? 0 : technique.trueQiBonus();
        if (technique != null) {
            for (String id : learnedTechniqueIds) {
                CultivationTechnique learned = CultivationTechniques.byId(id);
                if (learned != null && learned != technique
                        && learned.resonanceGroup().equals(technique.resonanceGroup())) {
                    bonus += learned.resonanceTrueQiBonus() * 5;
                }
            }
        }
        return realm.trueQiMaximumAt(realmLevel) + bonus;
    }

    public int passiveHealthRecoveryIntervalTicks() {
        if (!initialized) return 0;
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        int recoveryPercent = technique == null ? 100 : technique.passiveHealthRecoveryPercent();
        if (technique != null && technique.elementalAffinity().equals("木")) {
            recoveryPercent = Math.min(200, recoveryPercent + 25);
        }
        return realm.passiveHealthRecoveryIntervalTicksAt(realmLevel, constitution, recoveryPercent);
    }

    public int meditationQiPerSecondMilli() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (technique == null) return 0;
        int base = technique.effectiveMeditationQiPerSecondMilli(
                spiritualRoot, constitution, comprehension, fortune, realm, realmLevel);
        int resonance = 0;
        for (String id : learnedTechniqueIds) {
            CultivationTechnique learned = CultivationTechniques.byId(id);
            if (learned != null && learned != technique
                    && learned.resonanceGroup().equals(technique.resonanceGroup())) {
                resonance += learned.resonanceMeditationBonusPercent();
            }
        }
        int drawback = technique.drawbackMeditationPercent();
        return (int) Math.max(1L, (long) base * (100L + resonance - drawback) / 100L);
    }

    public int techniqueTrueQiRecoveryPerSecond() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (technique == null) return 0;
        int recovery = technique.trueQiRecoveryPerSecond();
        for (String id : learnedTechniqueIds) {
            CultivationTechnique learned = CultivationTechniques.byId(id);
            if (learned != null && learned != technique
                    && learned.resonanceGroup().equals(technique.resonanceGroup())) {
                recovery += learned.resonanceTrueQiBonus();
            }
        }
        return Math.max(0, recovery - technique.drawbackTrueQiCostPercent() * recovery / 100);
    }

    public int techniqueCombatAttackBonus() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (technique == null) return 0;
        int bonus = technique.combatAttackBonus();
        for (String id : learnedTechniqueIds) {
            CultivationTechnique learned = CultivationTechniques.byId(id);
            if (learned != null && learned != technique
                    && learned.resonanceGroup().equals(technique.resonanceGroup())) {
                bonus += learned.resonanceTrueQiBonus();
            }
        }
        return bonus;
    }

    public double techniqueHealthBonus() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return technique == null ? 0.0D : technique.healthBonus()
                + (learnedTechniqueIds.size() > 1 && technique.isUniversal() ? 4.0D : 0.0D);
    }

    public float techniqueDamageReduction() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return technique == null ? 0.0F : technique.damageReduction()
                + (learnedTechniqueIds.size() > 1 && "body".equals(technique.resonanceGroup()) ? 0.02F : 0.0F);
    }

    public int healthRecoveryTrueQiCost() {
        int baseCost = 6 + realm.ordinal() * 4 + Math.max(0, realmLevel - 1) / 3;
        int constitutionDiscount = Math.min(40, constitution / 3);
        return Math.max(1, baseCost * (100 - constitutionDiscount) / 100);
    }

    public boolean isTrueQiHealthRecovery() {
        return trueQiHealthRecovery;
    }

    public void setTrueQiHealthRecovery(boolean value) {
        trueQiHealthRecovery = value;
    }

    public int hungerTrueQiCost() {
        int stageDiscount = Math.max(0, realmLevel - 1) / 2;
        return Math.max(1, 10 - realm.ordinal() * 2 - spiritualRoot / 30 - stageDiscount);
    }

    public boolean hasTaixuAnchor() {
        return hasTaixuAnchor;
    }

    public String taixuOriginDimension() {
        return taixuOriginDimension;
    }

    public double taixuOriginX() {
        return taixuOriginX;
    }

    public double taixuOriginY() {
        return taixuOriginY;
    }

    public double taixuOriginZ() {
        return taixuOriginZ;
    }

    public float taixuOriginYaw() {
        return taixuOriginYaw;
    }

    public float taixuOriginPitch() {
        return taixuOriginPitch;
    }

    public float taixuOriginFlyingSpeed() {
        return taixuOriginFlyingSpeed;
    }

    public boolean taixuOriginMayfly() {
        return taixuOriginMayfly;
    }

    public boolean taixuOriginFlying() {
        return taixuOriginFlying;
    }

    public void setTaixuAnchor(String dimension, double x, double y, double z, float yaw,
                               float pitch, float flyingSpeed, boolean mayfly, boolean flying) {
        hasTaixuAnchor = true;
        taixuOriginDimension = dimension;
        taixuOriginX = x;
        taixuOriginY = y;
        taixuOriginZ = z;
        taixuOriginYaw = yaw;
        taixuOriginPitch = pitch;
        taixuOriginFlyingSpeed = flyingSpeed;
        taixuOriginMayfly = mayfly;
        taixuOriginFlying = flying;
    }

    public void clearTaixuAnchor() {
        hasTaixuAnchor = false;
        taixuOriginDimension = "";
        taixuOriginX = 0.0D;
        taixuOriginY = 0.0D;
        taixuOriginZ = 0.0D;
        taixuOriginYaw = 0.0F;
        taixuOriginPitch = 0.0F;
        taixuOriginFlyingSpeed = 0.05F;
        taixuOriginMayfly = false;
        taixuOriginFlying = false;
    }

    public boolean foodLevelDecreasedTo(int foodLevel) {
        boolean decreased = lastObservedFoodLevel >= 0 && foodLevel < lastObservedFoodLevel;
        lastObservedFoodLevel = foodLevel;
        return decreased;
    }

    public void synchronizeObservedFoodLevel(int foodLevel) {
        lastObservedFoodLevel = foodLevel;
    }

    public int passiveTrueQiRecoveryPerTenSeconds() {
        if (!initialized) return 0;
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        if (technique == null) return 0;
        int recovery = realm.passiveTrueQiRecoveryPerTenSecondsAt(
                realmLevel, comprehension, techniqueTrueQiRecoveryPerSecond());
        return technique.elementalAffinity().equals("水") && recovery > 0
                ? recovery + Math.max(1, recovery / 3) : recovery;
    }

    public int alchemyLevel() { return alchemyLevel; }

    public int alchemyExperience() { return alchemyExperience; }

    public int alchemyExperienceToNextLevel() { return alchemyLevel * 40; }

    public void restoreTrueQi(int amount) {
        if (amount > 0) trueQi = Math.min(trueQiMaximum(), trueQi + amount);
    }

    public boolean spendTrueQi(int amount) {
        if (amount < 0 || trueQi < amount) return false;
        trueQi -= amount;
        return true;
    }

    public void clampTrueQi() { trueQi = Math.min(trueQi, trueQiMaximum()); }

    public void addAlchemyExperience(int amount) {
        if (amount <= 0 || alchemyLevel >= 100) return;
        alchemyExperience += amount;
        while (alchemyLevel < 100 && alchemyExperience >= alchemyExperienceToNextLevel()) {
            alchemyExperience -= alchemyExperienceToNextLevel();
            alchemyLevel = Math.min(100, alchemyLevel + 1);
        }
        if (alchemyLevel >= 100) alchemyExperience = 0;
    }

    public int spiritualRoot() {
        return spiritualRoot;
    }

    public int constitution() {
        return constitution;
    }

    public int comprehension() {
        return comprehension;
    }

    public int fortune() {
        return fortune;
    }

    public String immortalFoundation() { return immortalFoundation; }

    public int majorBreakthroughFailures() { return majorBreakthroughFailures; }

    public boolean canUseJumpBoostAt(int tick) {
        if (tick - lastJumpBoostTick < 16) return false;
        lastJumpBoostTick = tick;
        return true;
    }

    public boolean isMeditating() {
        return meditating;
    }

    public boolean isStudyingTechnique() {
        return !studyingTechniqueId.isEmpty();
    }

    public String studyingTechniqueId() {
        return studyingTechniqueId;
    }

    public int techniqueStudyTicks() {
        return techniqueStudyTicks;
    }

    public int techniqueStudyDuration() {
        return techniqueStudyDuration;
    }

    public int techniqueStudyChance() {
        return techniqueStudyChance;
    }

    public int techniqueStudyRoll() {
        return techniqueStudyRoll;
    }

    public float techniqueStudyProgress() {
        return techniqueStudyDuration <= 0 ? 0.0F
                : Math.min(1.0F, techniqueStudyTicks / (float) techniqueStudyDuration);
    }

    public boolean hasLearnedTechnique() {
        return learnedTechniqueIds.contains(techniqueId)
                && CultivationTechniques.byId(techniqueId) != null;
    }

    public boolean hasLearnedTechnique(String id) {
        return learnedTechniqueIds.contains(id);
    }

    /** Selects a previously learned manual without discarding the other lineages. */
    public boolean activateTechnique(String id) {
        CultivationTechnique technique = CultivationTechniques.byId(id);
        if (technique == null || !learnedTechniqueIds.contains(id)
                || !technique.isCompatibleWithPath(cultivationPath)) {
            return false;
        }
        techniqueId = id;
        clampTrueQi();
        return true;
    }

    public boolean learnTechnique(String id) {
        CultivationTechnique technique = CultivationTechniques.byId(id);
        if (technique == null || !technique.canBeLearnedAt(realm)
                || !technique.isCompatibleWithPath(cultivationPath)) {
            return false;
        }
        learnedTechniqueIds.add(id);
        techniqueId = id;
        clampTrueQi();
        return true;
    }

    public CultivationRealm breakthroughTargetRealm() {
        return realmLevel == realm.levelCount() ? realm.next() : realm;
    }

    public boolean canBreakthroughWithCurrentTechnique() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        CultivationRealm targetRealm = breakthroughTargetRealm();
        if (technique == null || targetRealm == null || !technique.canBeLearnedAt(realm)
                || !technique.canCultivateTo(targetRealm)
                || !technique.isCompatibleWithPath(cultivationPath)) return false;
        return targetRealm != CultivationRealm.PURPLE_MANSION
                || technique.matchesImmortalFoundation(immortalFoundation);
    }

    public int learningChance(CultivationTechnique technique) {
        if (technique.learningDifficulty() == 0) {
            return 98;
        }
        int chance = 100 - technique.learningDifficulty() * 8
                + comprehension / 3 + fortune / 5 + spiritualRoot / 10
                + Math.max(0, realmLevel - 1);
        return Math.max(10, Math.min(98, chance));
    }

    public boolean beginTechniqueStudy(String id, int duration, int chance, int roll,
                                       double x, double y, double z, boolean wasCrouching) {
        CultivationTechnique technique = CultivationTechniques.byId(id);
        if (!initialized || meditating || isStudyingTechnique() || hasLearnedTechnique(id)
                || technique == null || !technique.canBeLearnedAt(realm)
                || !technique.isCompatibleWithPath(cultivationPath)) {
            return false;
        }
        studyingTechniqueId = id;
        techniqueStudyTicks = 0;
        techniqueStudyDuration = Math.max(1, duration);
        techniqueStudyChance = Math.max(1, Math.min(98, chance));
        techniqueStudyRoll = Math.max(1, Math.min(100, roll));
        meditationAnchorX = x;
        meditationAnchorY = y;
        meditationAnchorZ = z;
        wasCrouchingBeforeMeditation = wasCrouching;
        return true;
    }

    public boolean tickTechniqueStudy() {
        if (!isStudyingTechnique()) {
            return false;
        }
        techniqueStudyTicks = Math.min(techniqueStudyDuration, techniqueStudyTicks + 1);
        return techniqueStudyTicks >= techniqueStudyDuration;
    }

    public void stopTechniqueStudy() {
        studyingTechniqueId = "";
        techniqueStudyTicks = 0;
        techniqueStudyDuration = 0;
        techniqueStudyChance = 0;
        techniqueStudyRoll = 0;
    }

    public String techniqueName() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return technique == null ? "未识功法" : technique.displayName();
    }

    public double meditationAnchorX() {
        return meditationAnchorX;
    }

    public double meditationAnchorY() {
        return meditationAnchorY;
    }

    public double meditationAnchorZ() {
        return meditationAnchorZ;
    }

    public boolean wasCrouchingBeforeMeditation() {
        return wasCrouchingBeforeMeditation;
    }

    public int breakthroughCost() {
        int baseCost = realm.breakthroughCost(realmLevel);
        int comprehensionPercent = Math.max(50, 110 - comprehension);
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        int techniquePercent = technique == null ? 100 : technique.breakthroughCostPercent();
        long adjustedCost = (long) baseCost * comprehensionPercent * techniquePercent / 10_000L;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, adjustedCost));
    }

    public int breakthroughChance() {
        CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
        return realm.breakthroughChanceAt(realmLevel, spiritualRoot, comprehension, fortune,
                technique == null ? 100 : technique.breakthroughCostPercent(),
                technique == null ? 0 : technique.breakthroughChanceBonus(), majorBreakthroughFailures);
    }

    public void addQi(int amount) {
        if (amount > 0) {
            qi = (int) Math.min(Integer.MAX_VALUE, (long) qi + amount);
        }
    }

    public void begin(FamilyOrigin familyOrigin, CultivationPath cultivationPath, RandomSource random) {
        this.initialized = true;
        this.familyOrigin = familyOrigin;
        this.cultivationPath = cultivationPath;
        boolean startsWithManual = familyOrigin.receivesStartingManual(cultivationPath);
        this.techniqueId = startsWithManual ? STARTING_TECHNIQUE : "";
        learnedTechniqueIds.clear();
        if (startsWithManual) learnedTechniqueIds.add(STARTING_TECHNIQUE);
        this.realm = CultivationRealm.FETAL_BREATH;
        this.realmLevel = 1;
        this.qi = 0;
        this.trueQi = realm.trueQiMaximumAt(realmLevel);
        this.immortalFoundation = "";
        this.majorBreakthroughFailures = 0;
        this.alchemyLevel = 1;
        this.alchemyExperience = 0;
        this.lastObservedFoodLevel = -1;
        clearTaixuAnchor();
        CultivationAttributeBonuses bonuses = familyOrigin.bonuses().plus(cultivationPath.bonuses());
        spiritualRoot = rollAttribute(random, bonuses.spiritualRoot());
        constitution = rollAttribute(random, bonuses.constitution());
        comprehension = rollAttribute(random, bonuses.comprehension());
        fortune = rollAttribute(random, bonuses.fortune());
        stopMeditating();
        stopTechniqueStudy();
    }

    public void startMeditating(double x, double y, double z, boolean wasCrouching) {
        meditating = true;
        meditationAnchorX = x;
        meditationAnchorY = y;
        meditationAnchorZ = z;
        wasCrouchingBeforeMeditation = wasCrouching;
        stillTicks = 0;
    }

    public void stopMeditating() {
        meditating = false;
        wasCrouchingBeforeMeditation = false;
        stillTicks = 0;
        meditationQiRemainder = 0;
    }

    public boolean tickMeditation() {
        if (!meditating) {
            return false;
        }

        stillTicks++;
        if (stillTicks >= 20) {
            stillTicks = 0;
            CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
            if (technique == null) {
                return false;
            }
            meditationQiRemainder += meditationQiPerSecondMilli();
            int qiGain = meditationQiRemainder / 1000;
            meditationQiRemainder %= 1000;
            addQi(qiGain);
            CultivationTechnique activeTechnique = CultivationTechniques.byId(techniqueId);
            restoreTrueQi(activeTechnique == null ? 1 : techniqueTrueQiRecoveryPerSecond());
            return true;
        }
        return false;
    }

    public BreakthroughResult breakthrough(RandomSource random) {
        int cost = breakthroughCost();
        if (!initialized || qi < cost || !canBreakthroughWithCurrentTechnique()) {
            return new BreakthroughResult(false, false, breakthroughChance(), 0,
                    majorBreakthroughFailures, 0, false);
        }

        CultivationRealm nextRealm = breakthroughTargetRealm();
        if (nextRealm == null) {
            return new BreakthroughResult(false, false, 0, 0, majorBreakthroughFailures, 0, false);
        }

        boolean majorBreakthrough = nextRealm != realm;
        int chance = breakthroughChance();
        if (majorBreakthrough && random.nextInt(100) + 1 > chance) {
            int failureLossPercent = Math.min(60, 20 + realm.ordinal() * 10);
            int lostQi = Math.max(1, (int) ((long) cost * failureLossPercent / 100L));
            qi = Math.max(0, qi - lostQi);
            int fatalRisk = realm.fatalBreakthroughRiskChance(majorBreakthroughFailures);
            boolean fatal = fatalRisk > 0 && random.nextInt(100) < fatalRisk;
            majorBreakthroughFailures++;
            return new BreakthroughResult(false, true, chance, lostQi,
                    majorBreakthroughFailures, fatalRisk, fatal);
        }

        CultivationRealm oldRealm = realm;
        String foundationBeingFormed = CultivationTechniques.byId(techniqueId).elementalAffinity();
        qi -= cost;
        if (realmLevel == realm.levelCount()) {
            realm = nextRealm;
            realmLevel = 1;
            if (realm == CultivationRealm.FOUNDATION_ESTABLISHMENT) {
                immortalFoundation = foundationBeingFormed;
            }
        } else {
            realmLevel++;
        }
        if (oldRealm != realm) majorBreakthroughFailures = 0;
        clampTrueQi();
        restoreTrueQi(trueQiMaximum() / 2);
        return new BreakthroughResult(true, majorBreakthrough, chance, 0, 0, 0, false);
    }

    public record BreakthroughResult(boolean success, boolean major, int chance, int lostQi,
                                     int failures, int fatalRiskChance, boolean fatal) {}

    public void copyFrom(CultivationData source) {
        deserializeNBT(source.serializeNBT());
        lastObservedFoodLevel = -1;
    }

    /** Applies a creative-only test pill without exposing a normal recipe path. */
    public boolean grantDirectRealm(CultivationRealm target, RandomSource random) {
        if (!initialized || target == null) return false;
        java.util.List<CultivationTechnique> eligible = CultivationTechniques.all().stream()
                .filter(technique -> technique.minimumRealm().ordinal() <= target.ordinal())
                .filter(technique -> technique.canCultivateTo(target))
                .toList();
        if (eligible.isEmpty()) return false;
        CultivationTechnique selected = eligible.get(random.nextInt(eligible.size()));
        realm = target;
        realmLevel = 1;
        qi = 0;
        trueQi = realm.trueQiMaximumAt(realmLevel);
        techniqueId = selected.id();
        learnedTechniqueIds.clear();
        learnedTechniqueIds.add(selected.id());
        immortalFoundation = target.ordinal() >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal()
                ? selected.elementalAffinity() : "";
        majorBreakthroughFailures = 0;
        stopMeditating();
        stopTechniqueStudy();
        clampTrueQi();
        return true;
    }

    public void resetForDeath() {
        initialized = false;
        familyOrigin = FamilyOrigin.MORTAL;
        cultivationPath = CultivationPath.WANDERER;
        techniqueId = STARTING_TECHNIQUE;
        learnedTechniqueIds.clear();
        realm = CultivationRealm.FETAL_BREATH;
        realmLevel = 1;
        qi = 0;
        trueQi = 0;
        alchemyLevel = 1;
        alchemyExperience = 0;
        spiritualRoot = BASE_ATTRIBUTE_MIN;
        constitution = BASE_ATTRIBUTE_MIN;
        comprehension = BASE_ATTRIBUTE_MIN;
        fortune = BASE_ATTRIBUTE_MIN;
        immortalFoundation = "";
        majorBreakthroughFailures = 0;
        lastJumpBoostTick = -1000;
        lastObservedFoodLevel = -1;
        clearTaixuAnchor();
        stopMeditating();
        stopTechniqueStudy();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION);
        tag.putBoolean("initialized", initialized);
        if (initialized) {
            tag.putString("race", "human");
            tag.putString("familyOrigin", familyOrigin.id());
            tag.putString("cultivationPath", cultivationPath.id());
            tag.putString("technique", techniqueId);
            ListTag learned = new ListTag();
            for (String id : learnedTechniqueIds) {
                learned.add(StringTag.valueOf(id));
            }
            tag.put("learnedTechniques", learned);
            tag.putString("realm", realm.id());
            tag.putInt("realmLevel", realmLevel);
            tag.putInt("qi", qi);
            tag.putInt("trueQi", trueQi);
            tag.putInt("alchemyLevel", alchemyLevel);
            tag.putInt("alchemyExperience", alchemyExperience);
            tag.putInt("spiritualRoot", spiritualRoot);
            tag.putInt("constitution", constitution);
            tag.putInt("comprehension", comprehension);
            tag.putInt("fortune", fortune);
            tag.putString("immortalFoundation", immortalFoundation);
            tag.putInt("majorBreakthroughFailures", majorBreakthroughFailures);
            if (hasTaixuAnchor) {
                tag.putBoolean("hasTaixuAnchor", true);
                tag.putString("taixuOriginDimension", taixuOriginDimension);
                tag.putDouble("taixuOriginX", taixuOriginX);
                tag.putDouble("taixuOriginY", taixuOriginY);
                tag.putDouble("taixuOriginZ", taixuOriginZ);
                tag.putFloat("taixuOriginYaw", taixuOriginYaw);
                tag.putFloat("taixuOriginPitch", taixuOriginPitch);
                tag.putFloat("taixuOriginFlyingSpeed", taixuOriginFlyingSpeed);
                tag.putBoolean("taixuOriginMayfly", taixuOriginMayfly);
                tag.putBoolean("taixuOriginFlying", taixuOriginFlying);
            }
            if (isStudyingTechnique()) {
                tag.putString("studyingTechnique", studyingTechniqueId);
                tag.putInt("techniqueStudyTicks", techniqueStudyTicks);
                tag.putInt("techniqueStudyDuration", techniqueStudyDuration);
                tag.putInt("techniqueStudyChance", techniqueStudyChance);
                tag.putInt("techniqueStudyRoll", techniqueStudyRoll);
                tag.putDouble("techniqueStudyX", meditationAnchorX);
                tag.putDouble("techniqueStudyY", meditationAnchorY);
                tag.putDouble("techniqueStudyZ", meditationAnchorZ);
                tag.putBoolean("techniqueStudyWasCrouching", wasCrouchingBeforeMeditation);
            }
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        lastObservedFoodLevel = -1;
        trueQiHealthRecovery = false;
        clearTaixuAnchor();
        boolean savedInitialized = tag.getBoolean("initialized");
        // Forge can deserialize a newly attached capability with an empty
        // tag during a player entity replacement.  That tag represents
        // "no data loaded yet", not a command to erase an already valid
        // cultivation identity.  Death/resetForDeath() explicitly clears
        // the in-memory flag, so a real reset still remains effective.
        if (!savedInitialized) {
            return;
        }
        initialized = true;

        int dataVersion = tag.getInt("dataVersion");

        FamilyOrigin savedFamily = FamilyOrigin.byId(tag.getString("familyOrigin"));
        familyOrigin = savedFamily == null ? FamilyOrigin.MORTAL : savedFamily;
        CultivationPath savedPath = CultivationPath.byId(tag.getString("cultivationPath"));
        cultivationPath = savedPath == null ? CultivationPath.WANDERER : savedPath;
        techniqueId = tag.contains("technique", Tag.TAG_STRING) ? tag.getString("technique") : STARTING_TECHNIQUE;
        if (!techniqueId.isBlank() && CultivationTechniques.byId(techniqueId) == null) {
            techniqueId = STARTING_TECHNIQUE;
        }
        learnedTechniqueIds.clear();
        if (tag.contains("learnedTechniques", Tag.TAG_LIST)) {
            ListTag learned = tag.getList("learnedTechniques", Tag.TAG_STRING);
            for (int i = 0; i < learned.size(); i++) {
                String id = learned.getString(i);
                if (CultivationTechniques.byId(id) != null) learnedTechniqueIds.add(id);
            }
        }
        if (learnedTechniqueIds.isEmpty() && !techniqueId.isBlank()) {
            learnedTechniqueIds.add(techniqueId);
        }
        if (!learnedTechniqueIds.contains(techniqueId)) {
            techniqueId = learnedTechniqueIds.stream().findFirst().orElse("");
        }
        realm = dataVersion < 5
                ? migrateLegacyRealm(tag.getString("realm"))
                : CultivationRealm.byId(tag.getString("realm"));
        realmLevel = Math.max(1, Math.min(realm.levelCount(), tag.getInt("realmLevel")));
        qi = Math.max(0, tag.getInt("qi"));
        trueQi = tag.contains("trueQi") ? Math.max(0, tag.getInt("trueQi"))
                : realm.trueQiMaximumAt(realmLevel);
        alchemyLevel = Math.max(1, Math.min(100, tag.getInt("alchemyLevel")));
        alchemyExperience = Math.max(0, tag.getInt("alchemyExperience"));
        CultivationAttributeBonuses legacyBonuses = familyOrigin.bonuses().plus(cultivationPath.bonuses());
        spiritualRoot = readAttribute(tag, "spiritualRoot", dataVersion, legacyBonuses.spiritualRoot());
        constitution = readAttribute(tag, "constitution", dataVersion, legacyBonuses.constitution());
        comprehension = readAttribute(tag, "comprehension", dataVersion, legacyBonuses.comprehension());
        fortune = readAttribute(tag, "fortune", dataVersion, legacyBonuses.fortune());
        immortalFoundation = tag.getString("immortalFoundation");
        majorBreakthroughFailures = Math.max(0, tag.getInt("majorBreakthroughFailures"));
        if (tag.getBoolean("hasTaixuAnchor")) {
            String dimension = tag.getString("taixuOriginDimension");
            if (net.minecraft.resources.ResourceLocation.tryParse(dimension) != null) {
                setTaixuAnchor(dimension, tag.getDouble("taixuOriginX"), tag.getDouble("taixuOriginY"),
                        tag.getDouble("taixuOriginZ"), tag.getFloat("taixuOriginYaw"),
                        tag.getFloat("taixuOriginPitch"), tag.getFloat("taixuOriginFlyingSpeed"),
                        tag.getBoolean("taixuOriginMayfly"), tag.getBoolean("taixuOriginFlying"));
            }
        }
        if (immortalFoundation.isBlank() && realm.ordinal() >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal()) {
            CultivationTechnique technique = CultivationTechniques.byId(techniqueId);
            immortalFoundation = technique == null ? "" : technique.elementalAffinity();
        }
        clampTrueQi();
        stopMeditating();
        stopTechniqueStudy();
        restoreTechniqueStudy(tag);
    }

    private void restoreTechniqueStudy(CompoundTag tag) {
        if (!tag.contains("studyingTechnique", Tag.TAG_STRING)) {
            return;
        }
        String id = tag.getString("studyingTechnique");
        CultivationTechnique technique = CultivationTechniques.byId(id);
        if (technique == null || hasLearnedTechnique(id)) {
            return;
        }

        studyingTechniqueId = id;
        techniqueStudyDuration = technique.learningDurationTicks();
        techniqueStudyTicks = Math.max(0, Math.min(techniqueStudyDuration, tag.getInt("techniqueStudyTicks")));
        techniqueStudyChance = Math.max(1, Math.min(98, tag.getInt("techniqueStudyChance")));
        techniqueStudyRoll = Math.max(1, Math.min(100, tag.getInt("techniqueStudyRoll")));
        meditationAnchorX = tag.getDouble("techniqueStudyX");
        meditationAnchorY = tag.getDouble("techniqueStudyY");
        meditationAnchorZ = tag.getDouble("techniqueStudyZ");
        wasCrouchingBeforeMeditation = tag.getBoolean("techniqueStudyWasCrouching");
    }

    private static int readAttribute(CompoundTag tag, String key, int dataVersion, int legacyBackgroundBonus) {
        if (!tag.contains(key)) {
            return Math.max(1, Math.min(MAX_ATTRIBUTE_SCORE, BASE_ATTRIBUTE_MIN + legacyBackgroundBonus));
        }
        int value = tag.getInt(key);
        if (dataVersion < 3) {
            value = value / 10 + legacyBackgroundBonus;
        } else if (dataVersion < 5) {
            value = (value - legacyBackgroundBonus) / 2 + legacyBackgroundBonus;
        }
        return Math.max(1, Math.min(MAX_ATTRIBUTE_SCORE, value));
    }

    private static int rollAttribute(RandomSource random, int backgroundBonus) {
        int roll = random.nextInt(BASE_ATTRIBUTE_MAX - BASE_ATTRIBUTE_MIN + 1) + BASE_ATTRIBUTE_MIN;
        return Math.max(1, Math.min(MAX_ATTRIBUTE_SCORE, roll + backgroundBonus));
    }

    private static CultivationRealm migrateLegacyRealm(String id) {
        return switch (id) {
            case "qi_refining" -> CultivationRealm.QI_REFINING;
            case "foundation_establishment" -> CultivationRealm.FOUNDATION_ESTABLISHMENT;
            case "golden_core" -> CultivationRealm.PURPLE_MANSION;
            case "nascent_soul" -> CultivationRealm.GOLDEN_CORE;
            case "spiritual_transformation", "void_refinement", "body_integration", "mahayana" ->
                    CultivationRealm.DAO_TAI;
            default -> CultivationRealm.byId(id);
        };
    }
}
