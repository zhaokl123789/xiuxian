package xiuxian.block;

public enum AlchemyFurnaceTier {
    MORTAL(1, "凡品炼丹炉", 600, 1),
    SPIRIT(2, "灵品炼丹炉", 480, 5),
    EARTH(3, "地品炼丹炉", 360, 15),
    HEAVEN(4, "天品炼丹炉", 240, 30);

    private final int level;
    private final String displayName;
    private final int defaultCookingTime;
    private final int requiredAlchemyLevel;

    AlchemyFurnaceTier(int level, String displayName, int defaultCookingTime, int requiredAlchemyLevel) {
        this.level = level;
        this.displayName = displayName;
        this.defaultCookingTime = defaultCookingTime;
        this.requiredAlchemyLevel = requiredAlchemyLevel;
    }

    public int level() {
        return level;
    }

    public String displayName() {
        return displayName;
    }

    public int defaultCookingTime() {
        return defaultCookingTime;
    }

    public int requiredAlchemyLevel() { return requiredAlchemyLevel; }

    public static AlchemyFurnaceTier byLevel(int level) {
        for (AlchemyFurnaceTier tier : values()) if (tier.level == level) return tier;
        return MORTAL;
    }
}
