package xiuxian.cultivation;

import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
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
        boolean changed = false;
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        double expectedHealth = 20.0D + data.realm().healthBonusAt(data.realmLevel())
                + data.techniqueHealthBonus();
        if (health != null) {
            if (health.getModifier(HEALTH_MODIFIER) != null) {
                health.removeModifier(HEALTH_MODIFIER);
                changed = true;
            }
            if (Math.abs(health.getBaseValue() - expectedHealth) > 0.0001D) {
                health.setBaseValue(expectedHealth);
                changed = true;
            }
        }
        changed |= update(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER, "\u4fee\u4e3a\uff1a\u653b\u4f10",
                data.realm().attackBonusAt(data.realmLevel()) + data.techniqueCombatAttackBonus());
        changed |= update(player, Attributes.ARMOR, ARMOR_MODIFIER, "\u4fee\u4e3a\uff1a\u62a4\u4f53",
                data.realm().armorBonusAt(data.realmLevel()));
        changed |= update(player, Attributes.MOVEMENT_SPEED, TECHNIQUE_MOVEMENT, "\u529f\u6cd5\u8eab\u6cd5",
                technique == null ? 0.0D : technique.movementSpeedBonus());
        double realmMovement = player.isSprinting() && data.trueQi() > 0
                ? 0.15D + data.realm().ordinal() * 0.08D : 0.0D;
        changed |= update(player, Attributes.MOVEMENT_SPEED, REALM_MOVEMENT, "\u4fee\u884c\u5954\u884c",
                realmMovement, AttributeModifier.Operation.MULTIPLY_TOTAL);
        clampHealth(player);
        if (changed) {
            sync(player);
        }
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
            player.setHealth(Math.min((float) newMaxHealth,
                    Math.max(0.0F, (float) (newMaxHealth * healthRatio))));
        } else if (oldHealth > newMaxHealth) {
            player.setHealth(Math.max(0.0F, (float) newMaxHealth));
        }
    }

    public static void applyAfterBreakthrough(ServerPlayer player, CultivationData data) {
        double oldMaxHealth = player.getMaxHealth();
        apply(player, data);
        double gainedMaxHealth = player.getMaxHealth() - oldMaxHealth;
        if (gainedMaxHealth > 0.0D) {
            double healAmount = Math.max(0.0D, Math.min(gainedMaxHealth,
                    player.getMaxHealth() - player.getHealth()));
            player.heal((float) healAmount);
        }
        sync(player);
    }

    public static void remove(ServerPlayer player) {
        remove(player, false);
    }

    public static void remove(ServerPlayer player, boolean movementLocked) {
        boolean changed = false;
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            changed |= remove(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
            if (Math.abs(health.getBaseValue() - 20.0D) > 0.0001D) {
                health.setBaseValue(20.0D);
                changed = true;
            }
        }
        changed |= remove(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER);
        changed |= remove(player, Attributes.ARMOR, ARMOR_MODIFIER);
        changed |= remove(player, Attributes.MOVEMENT_SPEED, TECHNIQUE_MOVEMENT);
        changed |= remove(player, Attributes.MOVEMENT_SPEED, REALM_MOVEMENT);
        setMeditating(player, movementLocked);
        clampHealth(player);
        if (changed) {
            sync(player);
        }
    }

    private static void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void setMeditating(ServerPlayer player, boolean meditating) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }

        AttributeModifier current = instance.getModifier(MEDITATION_MOVEMENT);
        if (meditating && current == null) {
            instance.addTransientModifier(new AttributeModifier(MEDITATION_MOVEMENT, "\u5165\u5b9a\u51cf\u901f", -1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            sync(player);
        } else if (!meditating && current != null) {
            instance.removeModifier(current);
            sync(player);
        }
    }

    private static boolean update(ServerPlayer player, Attribute attribute, UUID id, String name, double amount) {
        return update(player, attribute, id, name, amount, AttributeModifier.Operation.ADDITION);
    }

    private static boolean update(ServerPlayer player, Attribute attribute, UUID id, String name,
                                  double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return false;
        }

        AttributeModifier current = instance.getModifier(id);
        if (current != null && Math.abs(current.getAmount() - amount) < 0.0001D
                && current.getOperation() == operation) {
            return false;
        }
        if (current != null) {
            instance.removeModifier(current);
        }
        // Permanent modifiers survive player entity replacement and are still
        // explicitly refreshed from cultivation data after every transfer.
        instance.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
        return true;
    }

    private static boolean remove(ServerPlayer player, Attribute attribute, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current == null) {
            return false;
        }
        instance.removeModifier(current);
        return true;
    }

    /** Sends all client-syncable attributes after a transfer or respawn. */
    public static void sync(ServerPlayer player) {
        player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(),
                player.getAttributes().getSyncableAttributes()));
    }
}
