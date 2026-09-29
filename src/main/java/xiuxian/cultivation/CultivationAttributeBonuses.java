package xiuxian.cultivation;

public record CultivationAttributeBonuses(int spiritualRoot, int constitution, int comprehension, int fortune) {
    public static final CultivationAttributeBonuses NEUTRAL = new CultivationAttributeBonuses(0, 0, 0, 0);

    public CultivationAttributeBonuses plus(CultivationAttributeBonuses other) {
        return new CultivationAttributeBonuses(
                spiritualRoot + other.spiritualRoot,
                constitution + other.constitution,
                comprehension + other.comprehension,
                fortune + other.fortune);
    }

    public String summary() {
        return "灵根 " + signed(spiritualRoot) + "，根骨 " + signed(constitution)
                + "，悟性 " + signed(comprehension) + "，气运 " + signed(fortune);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
