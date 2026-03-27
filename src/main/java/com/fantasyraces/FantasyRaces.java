package com.fantasyraces;

import com.fantasyraces.commands.RaceCommand;
import com.fantasyraces.listeners.PlayerListener;
import com.fantasyraces.managers.LuckPermsManager;
import com.fantasyraces.managers.RaceManager;
import com.fantasyraces.managers.PlayerDataManager;
import com.fantasyraces.managers.ConditionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FantasyRaces extends JavaPlugin {

    private static FantasyRaces instance;
    private RaceManager raceManager;
    private PlayerDataManager playerDataManager;
    private LuckPermsManager luckPermsManager;
    private ConditionManager conditionManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Init managers
        this.raceManager = new RaceManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.luckPermsManager = new LuckPermsManager(this);
        this.conditionManager = new ConditionManager(this);

        raceManager.loadRaces();

        // Register command
        RaceCommand raceCommand = new RaceCommand(this);
        getCommand("race").setExecutor(raceCommand);
        getCommand("race").setTabCompleter(raceCommand);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Start condition tick task (every 2 seconds)
        conditionManager.startTask();

        getLogger().info("FantasyRaces enabled! Loaded " + raceManager.getRaceCount() + " races.");

        if (!luckPermsManager.isAvailable()) {
            getLogger().warning("LuckPerms not found — group sync disabled.");
        }
    }

    @Override
    public void onDisable() {
        if (conditionManager != null) conditionManager.stopTask();
        if (playerDataManager != null) playerDataManager.saveAll();
        getLogger().info("FantasyRaces disabled.");
    }

    public void reload() {
        reloadConfig();
        raceManager.loadRaces();
        conditionManager.stopTask();
        conditionManager.startTask();
    }

    public static FantasyRaces getInstance() { return instance; }
    public RaceManager getRaceManager() { return raceManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public LuckPermsManager getLuckPermsManager() { return luckPermsManager; }
    public ConditionManager getConditionManager() { return conditionManager; }

    public String colorize(String msg) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String prefix() {
        return colorize(getConfig().getString("messages.prefix", "&8[&6Races&8] &r"));
    }

    public String msg(String key) {
        return colorize(getConfig().getString("messages." + key, "&cMissing message: " + key));
    }

    public String msg(String key, String... replacements) {
        String s = msg(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace(replacements[i], replacements[i + 1]);
        }
        return s;
    }
}
