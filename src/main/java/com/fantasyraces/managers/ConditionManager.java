package com.fantasyraces.managers;

import com.fantasyraces.FantasyRaces;
import com.fantasyraces.models.Condition;
import com.fantasyraces.models.PassiveAbility;
import com.fantasyraces.models.Race;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;

public class ConditionManager {

    private static final int TICK_INTERVAL  = 40;  // evaluate every 2 seconds
    private static final int EFFECT_DURATION = 60; // potion effect lasts 3s (refreshed every 2s)

    private static final Set<String> COLD_BIOMES = Set.of(
        "FROZEN_OCEAN", "FROZEN_RIVER", "SNOWY_PLAINS", "SNOWY_MOUNTAINS",
        "SNOWY_BEACH", "SNOWY_TAIGA", "SNOWY_TAIGA_MOUNTAINS", "SNOWY_TAIGA_HILLS",
        "ICE_SPIKES", "FROZEN_PEAKS", "JAGGED_PEAKS", "STONY_PEAKS",
        "GROVE", "SNOWY_SLOPES", "DEEP_FROZEN_OCEAN", "COLD_OCEAN",
        "DEEP_COLD_OCEAN", "WINDSWEPT_GRAVELLY_HILLS"
    );

    private final FantasyRaces plugin;
    private BukkitTask task;

    public ConditionManager(FantasyRaces plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    public void stopTask() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            String raceId = plugin.getPlayerDataManager().getPlayerRace(uuid);
            if (raceId == null) continue;

            Race race = plugin.getRaceManager().getRace(raceId);
            if (race == null) continue;

            // Apply permanent passives
            for (PassiveAbility passive : race.getPassives()) {
                applyPotionEffect(player, passive);
            }

            // Evaluate conditions — ALL triggers in a condition must be true (AND logic)
            for (Condition condition : race.getConditions()) {
                if (allTriggersMatch(player, condition)) {
                    if (condition.getEffect() != null) {
                        applyPotionEffect(player, condition.getEffect());
                    }
                    if (condition.hasSpecialEffect()) {
                        applySpecialEffect(player, condition);
                    }
                }
            }
        }
    }

    /**
     * Returns true only if EVERY trigger in the condition is satisfied.
     */
    private boolean allTriggersMatch(Player player, Condition condition) {
        for (Condition.Trigger trigger : condition.getTriggers()) {
            if (!evaluateSingleTrigger(player, trigger, condition)) return false;
        }
        return !condition.getTriggers().isEmpty();
    }

    private boolean evaluateSingleTrigger(Player player, Condition.Trigger trigger, Condition condition) {
        return switch (trigger) {
            case BELOW_Y      -> player.getLocation().getY() < condition.getYValue();
            case ABOVE_Y      -> player.getLocation().getY() > condition.getYValue();
            case IS_DAYTIME   -> {
                long time = player.getWorld().getTime();
                yield time >= 0 && time < 13000;
            }
            case IS_NIGHTTIME -> {
                long time = player.getWorld().getTime();
                yield time >= 13000 && time < 24000;
            }
            case IN_WATER     -> player.isInWater();
            case ON_FIRE      -> player.getFireTicks() > 0;
            case IN_BIOME     -> {
                String biomeKey = player.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase();
                yield condition.getBiomes() != null &&
                        condition.getBiomes().stream()
                                .anyMatch(b -> b.equalsIgnoreCase(biomeKey));
            }
            case WORLD_NAME   -> {
                String worldName = condition.getWorldName();
                yield worldName != null &&
                        player.getWorld().getName().equalsIgnoreCase(worldName);
            }
            case IN_NETHER    -> player.getWorld().getEnvironment() == World.Environment.NETHER;
            case IN_END       -> player.getWorld().getEnvironment() == World.Environment.THE_END;
            case IN_OVERWORLD -> player.getWorld().getEnvironment() == World.Environment.NORMAL;
            case IN_COLD_BIOME -> {
                String biomeKey = player.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase();
                yield COLD_BIOMES.contains(biomeKey.toUpperCase());
            }
        };
    }

    private void applyPotionEffect(Player player, PassiveAbility ability) {
        PotionEffectType pet = ability.toBukkitType();
        if (pet == null) return;
        PotionEffect existing = player.getPotionEffect(pet);
        if (existing != null && existing.getAmplifier() >= ability.getAmplifier()
                && existing.getDuration() > 20) return;
        player.addPotionEffect(new PotionEffect(pet, EFFECT_DURATION, ability.getAmplifier(), true, false, false));
    }

    private void applySpecialEffect(Player player, Condition condition) {
        switch (condition.getSpecialEffect()) {
            case SET_ON_FIRE -> {
                if (player.getFireTicks() <= 0
                        && player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE) == null) {
                    player.setFireTicks(TICK_INTERVAL + 20);
                }
            }
            case FREEZE -> player.setFreezeTicks(player.getMaxFreezeTicks());
            case EXTINGUISH -> player.setFireTicks(0);
            case DAMAGE -> {
                double dmg = condition.getDamageAmount();
                if (dmg > 0) player.damage(dmg);
            }
            case NONE -> { }
        }
    }

    public void removeRaceEffects(Player player, Race race) {
        if (race == null) return;
        for (PassiveAbility passive : race.getPassives()) {
            player.removePotionEffect(passive.toBukkitType());
        }
        for (Condition condition : race.getConditions()) {
            if (condition.getEffect() != null) {
                player.removePotionEffect(condition.getEffect().toBukkitType());
            }
            if (condition.hasSpecialEffect()) {
                switch (condition.getSpecialEffect()) {
                    case SET_ON_FIRE -> player.setFireTicks(0);
                    case FREEZE      -> player.setFreezeTicks(0);
                    default          -> { }
                }
            }
        }
    }
}
