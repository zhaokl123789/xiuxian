package xiuxian.cultivation;

import java.util.Arrays;

public enum CultivationRealm {
    FETAL_BREATH("fetal_breath", "胎息", 6, "轮"),
    QI_REFINING("qi_refining", "炼气", 9, "层"),
    FOUNDATION_ESTABLISHMENT("foundation_establishment", "筑基（道人）", 9, "层"),
    PURPLE_MANSION("purple_mansion", "紫府（真人）", 9, "层"),
    GOLDEN_CORE("golden_core", "金丹（真君）", 9, "层"),
    DAO_TAI("dao_tai", "道胎（仙君）", 9, "层");

    private static final double[] HEALTH_BASE = {0, 25, 60, 300, 700, 1500};
    private static final double[] HEALTH_PER_STAGE = {2, 8, 18, 20, 75, 140};
    private static final double[] ATTACK_BASE = {0, 4, 50, 300, 1000, 4000};
    private static final double[] ATTACK_PER_STAGE = {0.5D, 4, 25, 45, 120, 300};
    private static final double[] ARMOR_BASE = {0, 1, 6, 14, 30, 50};
    private static final double[] ARMOR_PER_STAGE = {0.1D, 0.4D, 1, 1, 4, 8};
    private static final double[] TRUE_QI_PER_STAGE = {10, 20, 40, 80, 160, 320};
    private static final double[] DAMAGE_REDUCTION_BASE = {0, 0.04D, 0.35D, 0.72D, 0.84D, 0.9D};
    private static final double[] DAMAGE_REDUCTION_PER_STAGE = {0.005D, 0.01D, 0.02D, 0.015D, 0.008D, 0.003D};
    private static final int[] HEALTH_REGEN_INTERVAL = {200, 140, 100, 60, 30, 20};
    private static final int[] TRUE_QI_REGEN_PER_TEN_SECONDS = {8, 20, 60, 180, 360, 720};

    private final String id;
    private final String displayName;
    private final int levelCount;
    private final String levelUnit;

    CultivationRealm(String id, String displayName, int levelCount, String levelUnit) {
        this.id = id;
        this.displayName = displayName;
        this.levelCount = levelCount;
        this.levelUnit = levelUnit;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int levelCount() {
        return levelCount;
    }

    public int completedBreakthroughs(int level) {
        int completed = 0;
        for (CultivationRealm previous : values()) {
            if (previous == this) {
                break;
            }
            completed += previous.levelCount;
        }
        return completed + Math.max(1, Math.min(level, levelCount)) - 1;
    }

    public double healthBonusAt(int level) {
        return HEALTH_BASE[ordinal()]
                + completedBreakthroughs(level)
                * HEALTH_PER_STAGE[ordinal()];
    }

    public double attackBonusAt(int level) {
        return ATTACK_BASE[ordinal()]
                + completedBreakthroughs(level)
                * ATTACK_PER_STAGE[ordinal()];
    }

    public double armorBonusAt(int level) {
        return ARMOR_BASE[ordinal()]
                + completedBreakthroughs(level)
                * ARMOR_PER_STAGE[ordinal()];
    }

    public int trueQiMaximumAt(int level) {
        long base = Math.round(100.0D * Math.pow(3.0D, ordinal()));
        long stageCapacity = Math.round(completedBreakthroughs(level) * TRUE_QI_PER_STAGE[ordinal()]);
        return (int) Math.min(Integer.MAX_VALUE, base + stageCapacity);
    }

    public float damageReductionAt(int level) {
        double reduction = DAMAGE_REDUCTION_BASE[ordinal()]
                + completedBreakthroughs(level)
                * DAMAGE_REDUCTION_PER_STAGE[ordinal()];
        return (float) Math.min(0.90D, reduction);
    }

    public int passiveHealthRegenerationIntervalTicks() {
        return HEALTH_REGEN_INTERVAL[ordinal()];
    }

    public int passiveHealthRecoveryIntervalTicksAt(int level, int constitution, int techniquePercent) {
        int baseInterval = passiveHealthRegenerationIntervalTicks();
        if (baseInterval <= 0 || techniquePercent <= 0) return 0;
        long stageFactor = 100L + (Math.max(1, Math.min(level, levelCount)) - 1L) * 4L;
        long constitutionFactor = 100L + Math.min(50, Math.max(0, constitution) / 2);
        long totalFactor = stageFactor * constitutionFactor * techniquePercent;
        return (int) Math.max(10L, (baseInterval * 1_000_000L + totalFactor - 1L) / totalFactor);
    }

    public int passiveTrueQiRecoveryPerTenSeconds() {
        return TRUE_QI_REGEN_PER_TEN_SECONDS[ordinal()];
    }

    public int passiveTrueQiRecoveryPerTenSecondsAt(int level, int comprehension, int techniqueRecoveryPerSecond) {
        int baseRecovery = passiveTrueQiRecoveryPerTenSeconds();
        if (baseRecovery <= 0 || techniqueRecoveryPerSecond <= 0) return 0;
        int stagePercent = 100 + (Math.max(1, Math.min(level, levelCount)) - 1) * 4;
        int comprehensionPercent = 100 + Math.min(50, Math.max(0, comprehension));
        int techniquePercent = 60 + techniqueRecoveryPerSecond * 15;
        long scaled = (long) baseRecovery * stagePercent * comprehensionPercent * techniquePercent;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE,
                (scaled + 1_000_000L - 1L) / 1_000_000L));
    }

    public String stageLabel(int level) {
        return "第 " + level + "/" + levelCount + " " + levelUnit;
    }

    public int breakthroughCost(int level) {
        double realmGrowth = 60.0D * Math.pow(8.0D, ordinal());
        double stageGrowth = Math.pow(1.48D, Math.max(0, Math.min(level, levelCount) - 1));
        double majorBreakthroughGrowth = level == levelCount && next() != null ? 3.0D + ordinal() * 1.2D : 1.0D;
        return (int) Math.min(Integer.MAX_VALUE,
                Math.max(1L, Math.round(realmGrowth * stageGrowth * majorBreakthroughGrowth)));
    }

    public int breakthroughChanceAt(int level, int spiritualRoot, int comprehension, int fortune,
                                     int techniqueCostPercent, int previousFailures) {
        if (level < levelCount) return 100;
        if (next() == null) return 0;
        int baseChance = switch (this) {
            case FETAL_BREATH -> 90;
            case QI_REFINING -> 60;
            case FOUNDATION_ESTABLISHMENT -> 28;
            case PURPLE_MANSION -> 12;
            case GOLDEN_CORE -> 5;
            case DAO_TAI -> 0;
        };
        int aptitudeBonus = (spiritualRoot * 2 + comprehension + fortune) / 8;
        int techniqueAdjustment = (100 - techniqueCostPercent) / 8;
        int cleanChance = Math.max(1, Math.min(95, baseChance + aptitudeBonus + techniqueAdjustment));
        double remainingChance = ordinal() < FOUNDATION_ESTABLISHMENT.ordinal()
                ? Math.pow(2.0D / 3.0D, Math.max(0, previousFailures))
                : Math.pow(0.55D, Math.max(0, previousFailures));
        return Math.max(1, (int) Math.floor(cleanChance * remainingChance));
    }

    public int fatalBreakthroughRiskChance(int previousFailures) {
        CultivationRealm target = next();
        if (target == null || target.ordinal() < FOUNDATION_ESTABLISHMENT.ordinal()) return 0;
        int baseRisk = switch (target) {
            case FOUNDATION_ESTABLISHMENT -> 12;
            case PURPLE_MANSION -> 22;
            case GOLDEN_CORE -> 35;
            case DAO_TAI -> 50;
            default -> 0;
        };
        return Math.min(85, baseRisk + Math.max(0, previousFailures) * 12);
    }

    public CultivationRealm next() {
        if (this == PURPLE_MANSION) return null;
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    public static CultivationRealm byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(FETAL_BREATH);
    }
}
