package com.fantasyraces.managers;

import com.fantasyraces.FantasyRaces;
import com.fantasyraces.models.Condition;
import com.fantasyraces.models.PassiveAbility;
import com.fantasyraces.models.Race;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class RaceManager {

    private final FantasyRaces plugin;
    private final Map<String, Race> races = new LinkedHashMap<>();

    public RaceManager(FantasyRaces plugin) {
        this.plugin = plugin;
    }

    public void loadRaces() {
        races.clear();

        ConfigurationSection racesSection = plugin.getConfig().getConfigurationSection("races");
        if (racesSection == null) {
            plugin.getLogger().warning("No 'races' section found in config.yml!");
            return;
        }

        for (String id : racesSection.getKeys(false)) {
            ConfigurationSection rs = racesSection.getConfigurationSection(id);
            if (rs == null) continue;

            try {
                Race race = parseRace(id, rs);
                races.put(id.toLowerCase(), race);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load race '" + id + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + races.size() + " race(s).");
    }

    private Race parseRace(String id, ConfigurationSection rs) {
        String displayName = rs.getString("display-name", id);
        String description = rs.getString("description", "");
        String icon        = rs.getString("icon", "STONE");
        String lpGroup     = rs.getString("luckperms-group", null);

        ConfigurationSection stats = rs.getConfigurationSection("stats");
        double maxHealth    = stats != null ? stats.getDouble("max-health", 0) : 0;
        double speed        = stats != null ? stats.getDouble("speed", 0) : 0;
        double damage       = stats != null ? stats.getDouble("damage", 0) : 0;
        double knockback    = stats != null ? stats.getDouble("knockback-resistance", 0) : 0;
        double scale        = stats != null ? stats.getDouble("scale", 1.0) : 1.0;

        List<PassiveAbility> passives = new ArrayList<>();
        List<?> passiveList = rs.getList("passives", Collections.emptyList());
        for (Object obj : passiveList) {
            if (obj instanceof Map<?, ?> map) {
                PassiveAbility pa = parseAbility(map);
                if (pa != null) passives.add(pa);
            }
        }

        List<Condition> conditions = new ArrayList<>();
        List<?> conditionList = rs.getList("conditions", Collections.emptyList());
        for (Object obj : conditionList) {
            if (obj instanceof Map<?, ?> map) {
                Condition c = parseCondition(map);
                if (c != null) conditions.add(c);
            }
        }

        return new Race(id.toLowerCase(), displayName, description, icon,
                lpGroup, maxHealth, speed, damage, knockback, scale, passives, conditions);
    }

    @SuppressWarnings("unchecked")
    private PassiveAbility parseAbility(Map<?, ?> map) {
        String typeName = (String) map.get("type");
        if (typeName == null) return null;
        try {
            PassiveAbility.Type type = PassiveAbility.Type.valueOf(typeName.toUpperCase());
            int amp = map.containsKey("amp") ? ((Number) map.get("amp")).intValue() : 0;
            return new PassiveAbility(type, amp);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown passive ability type: " + typeName);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Condition parseCondition(Map<?, ?> map) {
        String effectName        = (String) map.get("effect");
        String specialEffectName = (String) map.get("special-effect");

        if (effectName == null && specialEffectName == null) {
            plugin.getLogger().warning("A condition has no effect or special-effect — skipping.");
            return null;
        }

        try {
            // Support both single trigger: "trigger: ABOVE_Y"
            // and compound triggers: "triggers: [ABOVE_Y, IS_DAYTIME]"
            List<Condition.Trigger> triggers = new ArrayList<>();
            if (map.containsKey("triggers")) {
                List<String> triggerNames = (List<String>) map.get("triggers");
                for (String tn : triggerNames) {
                    try {
                        triggers.add(Condition.Trigger.valueOf(tn.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Unknown trigger: " + tn);
                    }
                }
            } else if (map.containsKey("trigger")) {
                String triggerName = (String) map.get("trigger");
                triggers.add(Condition.Trigger.valueOf(triggerName.toUpperCase()));
            } else {
                plugin.getLogger().warning("Condition has no trigger or triggers field — skipping.");
                return null;
            }

            if (triggers.isEmpty()) return null;

            // Parse potion effect (optional)
            PassiveAbility effect = null;
            if (effectName != null) {
                try {
                    PassiveAbility.Type effectType = PassiveAbility.Type.valueOf(effectName.toUpperCase());
                    int amp = map.containsKey("amp") ? ((Number) map.get("amp")).intValue() : 0;
                    effect = new PassiveAbility(effectType, amp);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown effect type: " + effectName);
                }
            }

            // Parse special effect (optional)
            Condition.SpecialEffect specialEffect = Condition.SpecialEffect.NONE;
            if (specialEffectName != null) {
                try {
                    specialEffect = Condition.SpecialEffect.valueOf(specialEffectName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown special-effect type: " + specialEffectName);
                }
            }

            double yValue      = map.containsKey("value")  ? ((Number) map.get("value")).doubleValue()  : 0;
            double damage      = map.containsKey("damage")  ? ((Number) map.get("damage")).doubleValue() : 0;
            String world       = (String) map.getOrDefault("world", null);
            List<String> biomes = map.containsKey("biomes") ? (List<String>) map.get("biomes") : Collections.emptyList();

            return new Condition(triggers, yValue, biomes, world, effect, specialEffect, damage);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid condition: " + e.getMessage());
            return null;
        }
    }

    public Race getRace(String id) {
        return id == null ? null : races.get(id.toLowerCase());
    }

    public Collection<Race> getAllRaces() {
        return Collections.unmodifiableCollection(races.values());
    }

    public int getRaceCount() {
        return races.size();
    }

    public boolean raceExists(String id) {
        return races.containsKey(id.toLowerCase());
    }
}
