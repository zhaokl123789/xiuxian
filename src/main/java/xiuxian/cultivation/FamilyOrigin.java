package xiuxian.cultivation;

import java.util.Arrays;

public enum FamilyOrigin {
    MORTAL("mortal", "凡俗家族", new CultivationAttributeBonuses(-3, 1, 0, 4)),
    CULTIVATOR("cultivator", "修行世家", new CultivationAttributeBonuses(6, -1, 3, -2)),
    FALLEN("fallen", "没落家族", new CultivationAttributeBonuses(2, 3, 4, -5));

    private final String id;
    private final String displayName;
    private final CultivationAttributeBonuses bonuses;

    FamilyOrigin(String id, String displayName, CultivationAttributeBonuses bonuses) {
        this.id = id;
        this.displayName = displayName;
        this.bonuses = bonuses;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public CultivationAttributeBonuses bonuses() {
        return bonuses;
    }

    public boolean receivesStartingManual(CultivationPath path) {
        return this != MORTAL || path == CultivationPath.SECT;
    }

    public String startingKitSummary(CultivationPath path) {
        boolean sect = path == CultivationPath.SECT;
        return switch (this) {
            case MORTAL -> sect
                    ? "入门功法、凝气丹、木镐与粗粮"
                    : "无功法、木镐与粗粮；可用书、纸、墨囊自制入门功法";
            case CULTIVATOR -> sect
                    ? "入门功法、2枚凝气丹、2块下品灵石"
                    : "入门功法、凝气丹、2块下品灵石与粗粮";
            case FALLEN -> sect
                    ? "入门功法、2枚凝气丹、祖传灵石与粗粮"
                    : "入门功法、凝气丹、祖传灵石与粗粮";
        };
    }

    public static FamilyOrigin byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(null);
    }

    public static String[] ids() {
        return Arrays.stream(values()).map(FamilyOrigin::id).toArray(String[]::new);
    }
}
