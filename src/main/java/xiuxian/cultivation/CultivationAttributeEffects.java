package xiuxian.cultivation;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class CultivationAttributeEffects {
    private static final UUID HEALTH_MODIFIER = UUID.fromString("c0b30001-4dc8-47a4-9281-8575ef9d6001");
    private static final UUID ATTACK_MODIFIER = UUID.fromString("c0b30002-4dc8-47a4-9281-8575ef9d6002");
    private static final UUID ARMOR_MODIFIER = UUID.fromString("c0b30003-4dc8-47a4-9281-8575ef9d6003");
    private static final UUID MEDITATION_MOVEMENT = UUID.fromString("c0b30004-4dc8-47a4-9281-8575ef9d6004");
    private static final UUID TECHNIQUE_MOVEMENT = UUID.fromString("c0b30005-4dc8-47a4-9281-8575ef9d6005");
    private static final UUID REALM_MOVEMENT = UUID.fromString("c0b30006-4dc8-47a4-9281-8575ef9d6006");

    private CultivationAttributeEffects() {}

    public static void apply(ServerPlayer player, CultivationData data) {
        CultivationTechnique technique = CultivationTechniques.byId(data.techniqueId());
        update(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER, "修为：气血",
                data.realm().healthBonusAt(data.realmLevel()) + (technique == null ? 0.0D : technique.healthBonus()));
        update(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER, "修为：攻击",
                data.realm().attackBonusAt(data.realmLevel()) + (technique == null ? 0 : technique.combatAttackBonus()));
        update(player, Attributes.ARMOR, ARMOR_MODIFIER, "修为：护体",
                data.realm().armorBonusAt(data.realmLevel()));
        update(player, Attributes.MOVEMENT_SPEED, TECHNIQUE_MOVEMENT, "Technique movement",
                technique == null ? 0.0D : technique.movementSpeedBonus());
        double realmMovement = player.isSprinting() && data.trueQi() > 0
                ? 0.15D + data.realm().ordinal() * 0.08D : 0.0D;
        update(player, Attributes.MOVEMENT_SPEED, REALM_MOVEMENT, "Cultivation sprint",
                realmMovement, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** Applies persisted cultivation attributes and restores health after a player reloads or respawns. */
    public static void applyAndPreserveHealth(ServerPlayer player, CultivationData data) {
        double oldMaxHealth = player.getMaxHealth();
        float oldHealth = player.getHealth();
        apply(player, data);
        double newMaxHealth = player.getMaxHealth();
        if (Math.abs(newMaxHealth - oldMaxHealth) > 0.0001D) {
            float healthRatio = oldMaxHealth <= 0.0D ? 1.0F : oldHealth / (float) oldMaxHealth;
            healthRatio = Math.max(0.0F, Math.min(1.0F, healthRatio));
            player.setHealth((float) (newMaxHealth * healthRatio));
        } else if (oldHealth > newMaxHealth) {
            player.setHealth((float) newMaxHealth);
        }
    }

    public static void applyAfterBreakthrough(ServerPlayer player, CultivationData data) {
        double oldMaxHealth = player.getMaxHealth();
        apply(player, data);
        double gainedMaxHealth = player.getMaxHealth() - oldMaxHealth;
        if (gainedMaxHealth > 0.0D) {
            player.heal((float) gainedMaxHealth);
        }
    }

    public static void remove(ServerPlayer player) {
        remove(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        remove(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER);
        remove(player, Attributes.ARMOR, ARMOR_MODIFIER);
        remove(player, Attributes.MOVEMENT_SPEED, TECHNIQUE_MOVEMENT);
        remove(player, Attributes.MOVEMENT_SPEED, REALM_MOVEMENT);
        setMeditating(player, false);
    }

    public static void setMeditating(ServerPlayer player, boolean meditating) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }

        AttributeModifier current = instance.getModifier(MEDITATION_MOVEMENT);
        if (meditating && current == null) {
            instance.addTransientModifier(new AttributeModifier(MEDITATION_MOVEMENT, "修行：入定", -1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!meditating && current != null) {
            instance.removeModifier(current);
        }
    }

    private static void update(ServerPlayer player, Attribute attribute, UUID id, String name, double amount) {
        update(player, attribute, id, name, amount, AttributeModifier.Operation.ADDITION);
    }

    private static void update(ServerPlayer player, Attribute attribute, UUID id, String name,
                               double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier current = instance.getModifier(id);
        if (current != null && Math.abs(current.getAmount() - amount) < 0.0001D) {
            return;
        }
        if (current != null) {
            instance.removeModifier(current);
        }
        instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
    }

    private static void remove(ServerPlayer player, Attribute attribute, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null) {
            instance.removeModifier(current);
        }
    }
}
