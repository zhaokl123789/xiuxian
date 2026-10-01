package xiuxian.cultivation;

/** A small, data-driven spell that can be learned from a cultivation lineage. */
public record CultivationSpell(String id, String displayName, String description,
                               CultivationRealm minimumRealm, CultivationRealm maximumRealm,
                               int trueQiCost, int cooldownTicks, int range, float magnitude,
                               int durationTicks, int amplifier, Effect effect, boolean targeted,
                               String requiredTechniqueId) {
    public enum Effect {
        DAMAGE, HEAL, EFFECT, RESTORE_TRUE_QI, CLEANSE, PUSH
    }

    public CultivationSpell {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank()
                || description == null || description.isBlank() || minimumRealm == null
                || maximumRealm == null || minimumRealm.ordinal() > maximumRealm.ordinal()
                || trueQiCost <= 0 || cooldownTicks < 0 || range < 0 || magnitude < 0.0F
                || durationTicks < 0 || amplifier < 0 || effect == null) {
            throw new IllegalArgumentException("Spell values must be valid");
        }
    }

    public boolean availableAt(CultivationData data) {
        if (data == null || !data.isInitialized() || data.realm() == null
                || data.realm().ordinal() < minimumRealm.ordinal()
                || data.realm().ordinal() > maximumRealm.ordinal()) {
            return false;
        }
        return requiredTechniqueId == null || requiredTechniqueId.equals(data.techniqueId());
    }

    public String usage() {
        return targeted ? "对准附近目标施展" : "对自身施展";
    }
}
