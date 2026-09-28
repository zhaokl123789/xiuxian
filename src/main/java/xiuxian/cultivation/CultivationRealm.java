package xiuxian.cultivation;

import java.util.Arrays;

public enum CultivationRealm {
    QI_REFINING("qi_refining", "炼气"),
    FOUNDATION_ESTABLISHMENT("foundation_establishment", "筑基"),
    GOLDEN_CORE("golden_core", "金丹"),
    NASCENT_SOUL("nascent_soul", "元婴"),
    SPIRITUAL_TRANSFORMATION("spiritual_transformation", "化神"),
    VOID_REFINEMENT("void_refinement", "炼虚"),
    BODY_INTEGRATION("body_integration", "合体"),
    MAHAYANA("mahayana", "大乘");

    private final String id;
    private final String displayName;

    CultivationRealm(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int breakthroughCost(int level) {
        return 30 + ordinal() * 30 + (level - 1) * 15;
    }

    public CultivationRealm next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    public static CultivationRealm byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(QI_REFINING);
    }
}
