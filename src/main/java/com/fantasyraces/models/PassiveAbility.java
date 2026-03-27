package com.fantasyraces.models;

import org.bukkit.potion.PotionEffectType;

public class PassiveAbility {

    public enum Type {
        NIGHT_VISION, FIRE_RESISTANCE, WATER_BREATHING,
        REGEN, STRENGTH, HASTE, SLOW_FALLING, GLOWING,
        SATURATION, DARKNESS, BLINDNESS, WEAKNESS,
        SLOWNESS, MINING_FATIGUE, POISON
    }

    private final Type type;
    private final int amplifier; // 0 = level I, 1 = level II, etc.

    public PassiveAbility(Type type, int amplifier) {
        this.type = type;
        this.amplifier = amplifier;
    }

    public Type getType() { return type; }
    public int getAmplifier() { return amplifier; }

    public PotionEffectType toBukkitType() {
        return switch (type) {
            case NIGHT_VISION    -> PotionEffectType.NIGHT_VISION;
            case FIRE_RESISTANCE -> PotionEffectType.FIRE_RESISTANCE;
            case WATER_BREATHING -> PotionEffectType.WATER_BREATHING;
            case REGEN           -> PotionEffectType.REGENERATION;
            case STRENGTH        -> PotionEffectType.STRENGTH;
            case HASTE           -> PotionEffectType.HASTE;
            case SLOW_FALLING    -> PotionEffectType.SLOW_FALLING;
            case GLOWING         -> PotionEffectType.GLOWING;
            case SATURATION      -> PotionEffectType.SATURATION;
            case DARKNESS        -> PotionEffectType.DARKNESS;
            case BLINDNESS       -> PotionEffectType.BLINDNESS;
            case WEAKNESS        -> PotionEffectType.WEAKNESS;
            case SLOWNESS        -> PotionEffectType.SLOWNESS;
            case MINING_FATIGUE  -> PotionEffectType.MINING_FATIGUE;
            case POISON          -> PotionEffectType.POISON;
        };
    }
}
