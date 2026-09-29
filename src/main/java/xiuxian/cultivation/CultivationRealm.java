package xiuxian.cultivation;

import java.util.Arrays;

public enum CultivationRealm {
    FETAL_BREATH("fetal_breath", "胎息", 6, "轮"),
    QI_REFINING("qi_refining", "炼气", 9, "层"),
    FOUNDATION_ESTABLISHMENT("foundation_establishment", "筑基（道人）", 9, "层"),
    PURPLE_MANSION("purple_mansion", "紫府（真人）", 9, "层"),
    GOLDEN_CORE("golden_core", "金丹（真君）", 9, "层"),
    DAO_TAI("dao_tai", "道胎（仙君）", 9, "层");

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
        return completedBreakthroughs(level) * 10.0D + ordinal() * 30.0D;
    }

    public double attackBonusAt(int level) {
        return completedBreakthroughs(level) * 1.25D + ordinal() * 4.0D;
    }

    public double armorBonusAt(int level) {
        return completedBreakthroughs(level) * 0.9D + ordinal() * 2.5D;
    }

    public int trueQiMaximumAt(int level) {
        return 100 + ordinal() * 250 + completedBreakthroughs(level) * 40;
    }

    public String stageLabel(int level) {
        return "第 " + level + "/" + levelCount + " " + levelUnit;
    }

    public int breakthroughCost(int level) {
        return 30 + ordinal() * 75 + (level - 1) * 8;
    }

    public CultivationRealm next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    public static CultivationRealm byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(FETAL_BREATH);
    }
}
