package xiuxian.cultivation;

import java.util.Arrays;

public enum CultivationPath {
    SECT("sect", "宗门弟子"),
    WANDERER("wanderer", "散修");

    private final String id;
    private final String displayName;

    CultivationPath(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static CultivationPath byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(null);
    }

    public static String[] ids() {
        return Arrays.stream(values()).map(CultivationPath::id).toArray(String[]::new);
    }
}
