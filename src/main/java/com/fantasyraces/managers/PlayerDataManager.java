package com.fantasyraces.managers;

import com.fantasyraces.FantasyRaces;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final FantasyRaces plugin;
    private final File dataFile;
    private YamlConfiguration data;

    // Cache: UUID -> raceId
    private final Map<UUID, String> raceCache = new HashMap<>();
    // Cache: UUID -> last select timestamp (ms)
    private final Map<UUID, Long> cooldownCache = new HashMap<>();

    public PlayerDataManager(FantasyRaces plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load all cached data
        if (data.contains("players")) {
            for (String uuidStr : data.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String race = data.getString("players." + uuidStr + ".race");
                long cooldown = data.getLong("players." + uuidStr + ".lastSelect", 0L);
                if (race != null) raceCache.put(uuid, race);
                cooldownCache.put(uuid, cooldown);
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, String> entry : raceCache.entrySet()) {
            String path = "players." + entry.getKey().toString();
            data.set(path + ".race", entry.getValue());
            data.set(path + ".lastSelect", cooldownCache.getOrDefault(entry.getKey(), 0L));
        }
        try { data.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public void save(UUID uuid) {
        String path = "players." + uuid.toString();
        data.set(path + ".race", raceCache.get(uuid));
        data.set(path + ".lastSelect", cooldownCache.getOrDefault(uuid, 0L));
        try { data.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public String getPlayerRace(UUID uuid) {
        return raceCache.get(uuid);
    }

    public void setPlayerRace(UUID uuid, String raceId) {
        raceCache.put(uuid, raceId);
        cooldownCache.put(uuid, System.currentTimeMillis());
        save(uuid);
    }

    public void clearPlayerRace(UUID uuid) {
        raceCache.remove(uuid);
        cooldownCache.remove(uuid);
        String path = "players." + uuid.toString();
        data.set(path, null);
        try { data.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public boolean isOnCooldown(UUID uuid) {
        int cooldownSec = plugin.getConfig().getInt("settings.reselect-cooldown", 0);
        if (cooldownSec <= 0) return false;
        Long last = cooldownCache.get(uuid);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < (cooldownSec * 1000L);
    }

    public long getRemainingCooldown(UUID uuid) {
        int cooldownSec = plugin.getConfig().getInt("settings.reselect-cooldown", 0);
        Long last = cooldownCache.get(uuid);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = (cooldownSec * 1000L) - elapsed;
        return Math.max(0, remaining / 1000);
    }
}
