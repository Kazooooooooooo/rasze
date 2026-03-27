package com.fantasyraces.models;

import java.util.List;

public class Race {

    private final String id;
    private final String displayName;
    private final String description;
    private final String iconMaterial;
    private final String luckPermsGroup;

    // Stats
    private final double maxHealthBonus;
    private final double speedModifier;
    private final double damageModifier;
    private final double knockbackResistance;
    private final double scale; // 1.0 = normal, 0.5 = half size, 2.0 = double size

    // Abilities
    private final List<PassiveAbility> passives;
    private final List<Condition> conditions;

    public Race(String id, String displayName, String description, String iconMaterial,
                String luckPermsGroup, double maxHealthBonus, double speedModifier,
                double damageModifier, double knockbackResistance, double scale,
                List<PassiveAbility> passives, List<Condition> conditions) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.iconMaterial = iconMaterial;
        this.luckPermsGroup = luckPermsGroup;
        this.maxHealthBonus = maxHealthBonus;
        this.speedModifier = speedModifier;
        this.damageModifier = damageModifier;
        this.knockbackResistance = knockbackResistance;
        this.scale = scale;
        this.passives = passives;
        this.conditions = conditions;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIconMaterial() { return iconMaterial; }
    public String getLuckPermsGroup() { return luckPermsGroup; }
    public double getMaxHealthBonus() { return maxHealthBonus; }
    public double getSpeedModifier() { return speedModifier; }
    public double getDamageModifier() { return damageModifier; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public double getScale() { return scale; }
    public List<PassiveAbility> getPassives() { return passives; }
    public List<Condition> getConditions() { return conditions; }
}
