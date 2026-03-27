package com.fantasyraces.models;

import java.util.List;

public class Condition {

    public enum Trigger {
        BELOW_Y, ABOVE_Y,
        IS_DAYTIME, IS_NIGHTTIME,
        IN_WATER, ON_FIRE,
        IN_BIOME, WORLD_NAME,
        IN_NETHER,
        IN_END,
        IN_OVERWORLD,
        IN_COLD_BIOME
    }

    public enum SpecialEffect {
        NONE,
        SET_ON_FIRE,
        FREEZE,
        EXTINGUISH,
        DAMAGE        // deals configurable damage every tick
    }

    // ALL triggers in this list must be true (AND logic)
    private final List<Trigger> triggers;

    private final double yValue;
    private final List<String> biomes;
    private final String worldName;

    private final PassiveAbility effect;
    private final SpecialEffect specialEffect;
    private final double damageAmount;   // HP per tick when specialEffect = DAMAGE

    public Condition(List<Trigger> triggers, double yValue, List<String> biomes,
                     String worldName, PassiveAbility effect, SpecialEffect specialEffect,
                     double damageAmount) {
        this.triggers      = triggers != null ? triggers : List.of();
        this.yValue        = yValue;
        this.biomes        = biomes != null ? biomes : List.of();
        this.worldName     = worldName;
        this.effect        = effect;
        this.specialEffect = specialEffect != null ? specialEffect : SpecialEffect.NONE;
        this.damageAmount  = damageAmount;
    }

    public List<Trigger> getTriggers()       { return triggers; }
    public double getYValue()                { return yValue; }
    public List<String> getBiomes()          { return biomes; }
    public String getWorldName()             { return worldName; }
    public PassiveAbility getEffect()        { return effect; }
    public SpecialEffect getSpecialEffect()  { return specialEffect; }
    public boolean hasSpecialEffect()        { return specialEffect != SpecialEffect.NONE; }
    public double getDamageAmount()          { return damageAmount; }
}
