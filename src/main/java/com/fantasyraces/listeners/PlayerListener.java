package com.fantasyraces.listeners;

import com.fantasyraces.FantasyRaces;
import com.fantasyraces.managers.AttributeManager;
import com.fantasyraces.models.Race;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final FantasyRaces plugin;

    public PlayerListener(FantasyRaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String raceId = plugin.getPlayerDataManager().getPlayerRace(player.getUniqueId());

        // Assign default race to new players
        if (raceId == null) {
            String defaultRace = plugin.getConfig().getString("settings.default-race", "none");
            if (!defaultRace.equalsIgnoreCase("none") && plugin.getRaceManager().raceExists(defaultRace)) {
                plugin.getPlayerDataManager().setPlayerRace(player.getUniqueId(), defaultRace);
                raceId = defaultRace;

                // Sync LuckPerms group
                Race race = plugin.getRaceManager().getRace(raceId);
                if (race != null && race.getLuckPermsGroup() != null) {
                    plugin.getLuckPermsManager().updatePlayerGroup(player.getUniqueId(), null, race.getLuckPermsGroup());
                }
            }
        }

        // Apply attributes on join (delayed 1 tick to ensure player is fully loaded)
        final String finalRaceId = raceId;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Race race = plugin.getRaceManager().getRace(finalRaceId);
            AttributeManager.applyRace(player, race);
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Nothing to clean up — data auto-saves
    }
}
