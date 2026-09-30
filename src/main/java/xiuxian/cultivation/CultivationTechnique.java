package xiuxian.cultivation;

public record CultivationTechnique(String id, String displayName, String doctrine, String method,
                                   CultivationRealm minimumRealm, CultivationRealm maximumRealm,
                                   int learningDifficulty, int meditationQiPerSecondMilli,
                                   Aptitude meditationAptitude, int breakthroughCostPercent,
                                   float damageReduction, double healthBonus, int passiveHealthRecoveryPercent,
                                   int trueQiBonus,
                                   int trueQiRecoveryPerSecond, float spellPowerMultiplier,
                                   int combatAttackBonus, double movementSpeedBonus,
                                   String fiveVirtue, String qiAffinity, String combatStyle) {
    public enum Aptitude {
        SPIRITUAL_ROOT("灵根"),
        CONSTITUTION("根骨"),
        COMPREHENSION("悟性"),
        FORTUNE("气运");

        private final String displayName;

        Aptitude(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public int value(int spiritualRoot, int constitution, int comprehension, int fortune) {
            return switch (this) {
                case SPIRITUAL_ROOT -> spiritualRoot;
                case CONSTITUTION -> constitution;
                case COMPREHENSION -> comprehension;
                case FORTUNE -> fortune;
            };
        }
    }

    public CultivationTechnique {
        if (id.isBlank() || displayName.isBlank() || doctrine.isBlank() || method.isBlank()) {
            throw new IllegalArgumentException("Technique text must not be blank");
        }
        if (minimumRealm == null || maximumRealm == null || minimumRealm.ordinal() > maximumRealm.ordinal()
                || learningDifficulty < 0 || learningDifficulty > 10
                || meditationQiPerSecondMilli <= 0 || meditationAptitude == null || breakthroughCostPercent <= 0
                || damageReduction < 0.0F || healthBonus < 0.0D
                || passiveHealthRecoveryPercent <= 0 || passiveHealthRecoveryPercent > 200 || trueQiBonus < 0
                || trueQiRecoveryPerSecond < 0 || spellPowerMultiplier <= 0.0F
                || combatAttackBonus < 0 || movementSpeedBonus < 0.0D || movementSpeedBonus > 0.05D
                || fiveVirtue.isBlank() || qiAffinity.isBlank() || combatStyle.isBlank()) {
            throw new IllegalArgumentException("Technique values must be non-negative and useful");
        }
    }

    public boolean canBeLearnedAt(CultivationRealm realm) {
        return realm != null && realm.ordinal() >= minimumRealm.ordinal()
                && realm.ordinal() <= maximumRealm.ordinal();
    }

    public boolean canCultivateTo(CultivationRealm realm) {
        return realm != null && realm.ordinal() <= maximumRealm.ordinal();
    }

    public String realmRangeLabel() {
        return minimumRealm.displayName() + "至" + maximumRealm.displayName();
    }

    public int learningDurationTicks() {
        return 100 + learningDifficulty * 80;
    }

    public int aptitudeMatchPercent(int spiritualRoot, int constitution, int comprehension, int fortune) {
        int aptitude = meditationAptitude.value(spiritualRoot, constitution, comprehension, fortune);
        return Math.min(250, 50 + aptitude * 5 + fortune / 3);
    }

    /**
     * Inherited manuals carry a real advantage at a major bottleneck.  The
     * cost discount remains useful for ordinary training, while this separate
     * score makes a sect lineage meaningfully better at crossing a realm wall.
     */
    public int breakthroughChanceBonus() {
        int inheritance = Math.max(0, maximumRealm.ordinal()) * 4;
        int refinement = Math.max(0, 100 - breakthroughCostPercent()) / 2;
        int breadth = Math.max(0, maximumRealm.ordinal() - minimumRealm.ordinal()) * 2;
        return Math.min(32, inheritance + refinement + breadth);
    }

    public int effectiveMeditationQiPerSecondMilli(int spiritualRoot, int constitution,
                                                    int comprehension, int fortune,
                                                    CultivationRealm realm, int realmLevel) {
        int matchPercent = aptitudeMatchPercent(spiritualRoot, constitution, comprehension, fortune);
        long aptitudeRate = (long) meditationQiPerSecondMilli * matchPercent / 100L + (long) fortune * 5L;
        int completedBreakthroughs = realm.completedBreakthroughs(realmLevel);
        long progressionPercent = 100L + completedBreakthroughs * 22L + realm.ordinal() * 100L;
        long effectiveRate = aptitudeRate * progressionPercent / 100L;
        return (int) Math.min(Integer.MAX_VALUE, effectiveRate);
    }

    public String elementalAffinity() {
        for (String element : new String[]{"木", "火", "土", "金", "水"}) {
            if (qiAffinity.contains(element)) return element;
        }
        return "无";
    }

    public boolean matchesImmortalFoundation(String foundation) {
        if (foundation == null || foundation.isBlank() || foundation.equals("无")) return false;
        String element = elementalAffinity();
        if (element.equals(foundation)) return true;
        return switch (element) {
            case "水" -> foundation.equals("木");
            case "木" -> foundation.equals("火");
            case "火" -> foundation.equals("土");
            case "土" -> foundation.equals("金");
            case "金" -> foundation.equals("水");
            default -> false;
        };
    }

    public String learningDifficultyLabel() {
        if (learningDifficulty == 0) return "入门";
        if (learningDifficulty <= 3) return "易学";
        if (learningDifficulty <= 6) return "进阶";
        if (learningDifficulty <= 8) return "艰深";
        return "晦涩";
    }

    public String effectSummary() {
        String affinityEffect = switch (elementalAffinity()) {
            case "木" -> "木炁养生：提升被动气血恢复";
            case "火" -> "火炁灼脉：近战命中消耗真炁并灼烧敌人";
            case "土" -> "土炁镇脉：额外降低近战与术法伤害";
            case "金" -> "金炁锐意：近战伤害提高";
            case "水" -> "水炁绵长：强化真炁周转";
            default -> "";
        };
        return "基础吐纳 " + String.format(java.util.Locale.ROOT, "%.2f", meditationQiPerSecondMilli / 1000.0D)
                + " 点/秒 · 每完成一层吐纳基础 +22%，每升一大境界额外 +100% · 契合属性："
                + meditationAptitude.displayName()
                + " · 真炁上限 +" + trueQiBonus + " · " + combatStyle + " · " + affinityEffect;
    }
}
