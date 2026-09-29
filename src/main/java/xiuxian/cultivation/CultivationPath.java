package xiuxian.cultivation;

import java.util.Arrays;

public enum CultivationPath {
    SECT("sect", "宗门弟子", new CultivationAttributeBonuses(1, 0, 2, -1)),
    WANDERER("wanderer", "散修", new CultivationAttributeBonuses(-1, 1, 0, 2));

    private final String id;
    private final String displayName;
    private final CultivationAttributeBonuses bonuses;

    CultivationPath(String id, String displayName, CultivationAttributeBonuses bonuses) {
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

    public static CultivationPath byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(null);
    }

    public static String[] ids() {
        return Arrays.stream(values()).map(CultivationPath::id).toArray(String[]::new);
    }
}
