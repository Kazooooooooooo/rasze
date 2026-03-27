package com.fantasyraces.managers;

import com.fantasyraces.models.Race;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

public class AttributeManager {

    // Use NamespacedKey-based AttributeModifier (1.21+ API)
    private static final NamespacedKey HEALTH_KEY    = new NamespacedKey("fantasyraces", "health");
    private static final NamespacedKey SPEED_KEY     = new NamespacedKey("fantasyraces", "speed");
    private static final NamespacedKey DAMAGE_KEY    = new NamespacedKey("fantasyraces", "damage");
    private static final NamespacedKey KNOCKBACK_KEY = new NamespacedKey("fantasyraces", "knockback");
    private static final NamespacedKey SCALE_KEY     = new NamespacedKey("fantasyraces", "scale");

    public static void applyRace(Player player, Race race) {
        removeRaceModifiers(player);
        if (race == null) return;

        // Max Health
        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null && race.getMaxHealthBonus() != 0) {
            health.addModifier(new AttributeModifier(
                    HEALTH_KEY,
                    race.getMaxHealthBonus() * 2,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY));
            if (player.getHealth() > health.getValue()) {
                player.setHealth(health.getValue());
            }
        }

        // Speed
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && race.getSpeedModifier() != 0) {
            speed.addModifier(new AttributeModifier(
                    SPEED_KEY,
                    race.getSpeedModifier(),
                    AttributeModifier.Operation.ADD_SCALAR,
                    EquipmentSlotGroup.ANY));
        }

        // Attack Damage
        AttributeInstance damage = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null && race.getDamageModifier() != 0) {
            damage.addModifier(new AttributeModifier(
                    DAMAGE_KEY,
                    race.getDamageModifier(),
                    AttributeModifier.Operation.ADD_SCALAR,
                    EquipmentSlotGroup.ANY));
        }

        // Knockback Resistance
        AttributeInstance knockback = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null && race.getKnockbackResistance() != 0) {
            knockback.addModifier(new AttributeModifier(
                    KNOCKBACK_KEY,
                    race.getKnockbackResistance(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY));
        }

        // Scale
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale != null && race.getScale() != 1.0) {
            scale.addModifier(new AttributeModifier(
                    SCALE_KEY,
                    race.getScale() - 1.0,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                    EquipmentSlotGroup.ANY));
        }
    }

    public static void removeRaceModifiers(Player player) {
        removeModifier(player.getAttribute(Attribute.MAX_HEALTH),        HEALTH_KEY);
        removeModifier(player.getAttribute(Attribute.MOVEMENT_SPEED),    SPEED_KEY);
        removeModifier(player.getAttribute(Attribute.ATTACK_DAMAGE),     DAMAGE_KEY);
        removeModifier(player.getAttribute(Attribute.KNOCKBACK_RESISTANCE), KNOCKBACK_KEY);
        removeModifier(player.getAttribute(Attribute.SCALE),             SCALE_KEY);
    }

    private static void removeModifier(AttributeInstance attr, NamespacedKey key) {
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(key))
                .findFirst()
                .ifPresent(attr::removeModifier);
    }
}
