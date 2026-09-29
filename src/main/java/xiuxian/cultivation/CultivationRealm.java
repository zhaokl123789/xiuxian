package xiuxian.cultivation;

import java.util.Arrays;

public enum CultivationRealm {
    FETAL_BREATH("fetal_breath", "胎息", 6, "轮"),
    QI_REFINING("qi_refining", "炼气", 9, "层"),
    FOUNDATION_ESTABLISHMENT("foundation_establishment", "筑基（道人）", 9, "层"),
    PURPLE_MANSION("purple_mansion", "紫府（真人）", 9, "层"),
    GOLDEN_CORE("golden_core", "金丹（真君）", 9, "层"),
    DAO_TAI("dao_tai", "道胎（仙君）", 9, "层");

    private static final double[] HEALTH_BASE = {0, 35, 100, 220, 360, 700};
    private static final double[] HEALTH_PER_STAGE = {4, 6, 8, 10, 10, 6};
    private static final double[] ATTACK_BASE = {0, 3, 12, 35, 90, 220};
    private static final double[] ATTACK_PER_STAGE = {0.75D, 1.5D, 3, 5, 9, 15};
    private static final double[] ARMOR_BASE = {0, 1, 2, 5, 8, 12};
    private static final double[] ARMOR_PER_STAGE = {0.1D, 0.15D, 0.25D, 0.35D, 0.3D, 0.25D};
    private static final double[] TRUE_QI_PER_STAGE = {10, 20, 40, 80, 160, 320};
    private static final double[] DAMAGE_REDUCTION_BASE = {0, 0.04D, 0.14D, 0.36D, 0.55D, 0.74D};
    private static final double[] DAMAGE_REDUCTION_PER_STAGE = {0.005D, 0.006D, 0.008D, 0.006D, 0.003D, 0.0005D};
    private static final int[] HEALTH_REGEN_INTERVAL = {0, 1200, 400, 100, 40, 15};
    private static final int[] TRUE_QI_REGEN_PER_TEN_SECONDS = {0, 2, 8, 30, 120, 480};

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
        return (float) Math.min(0.75D, reduction);
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
        double realmGrowth = 30.0D * Math.pow(2.2D, ordinal());
        double stageGrowth = Math.pow(1.18D, Math.max(0, level - 1));
        double majorBreakthroughGrowth = level == levelCount && next() != null
                ? 2.0D + ordinal() * 0.8D : 1.0D;
        return (int) Math.min(Integer.MAX_VALUE,
                Math.max(1L, Math.round(realmGrowth * stageGrowth * majorBreakthroughGrowth)));
    }

    public int breakthroughChanceAt(int level, int spiritualRoot, int comprehension, int fortune,
                                     int techniqueCostPercent) {
        if (next() == null) return 0;
        if (level < levelCount) return 100;
        int baseChance = switch (this) {
            case FETAL_BREATH -> 90;
            case QI_REFINING -> 60;
            case FOUNDATION_ESTABLISHMENT -> 28;
            case PURPLE_MANSION -> 12;
            case GOLDEN_CORE -> 5;
            case DAO_TAI -> 0;
        };
        int aptitudeBonus = (spiritualRoot + comprehension + fortune) / 20;
        int techniqueAdjustment = (100 - techniqueCostPercent) / 8;
        return Math.max(3, Math.min(95, baseChance + aptitudeBonus + techniqueAdjustment));
    }

    public CultivationRealm next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    public static CultivationRealm byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(FETAL_BREATH);
    }
}
