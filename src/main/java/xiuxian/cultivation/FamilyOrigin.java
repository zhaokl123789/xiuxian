package xiuxian.cultivation;

import java.util.Arrays;

public enum FamilyOrigin {
    MORTAL("mortal", "凡俗家族"),
    CULTIVATOR("cultivator", "修行世家"),
    FALLEN("fallen", "没落家族");

    private final String id;
    private final String displayName;

    FamilyOrigin(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static FamilyOrigin byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(null);
    }

    public static String[] ids() {
        return Arrays.stream(values()).map(FamilyOrigin::id).toArray(String[]::new);
    }
}
