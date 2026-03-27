package com.fantasyraces.managers;

import com.fantasyraces.FantasyRaces;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsManager {

    private final FantasyRaces plugin;
    private LuckPerms luckPerms;

    public LuckPermsManager(FantasyRaces plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                this.luckPerms = provider.getProvider();
            }
        }
    }

    public boolean isAvailable() {
        return luckPerms != null && plugin.getConfig().getBoolean("settings.luckperms-sync", true);
    }

    /**
     * Removes the old race group and adds the new race group for a player.
     */
    public CompletableFuture<Void> updatePlayerGroup(UUID uuid, String oldGroupName, String newGroupName) {
        if (!isAvailable()) return CompletableFuture.completedFuture(null);

        return luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;

            // Remove old race group
            if (oldGroupName != null && !oldGroupName.isEmpty()) {
                InheritanceNode oldNode = InheritanceNode.builder(oldGroupName).build();
                user.data().remove(oldNode);
            }

            // Add new race group
            if (newGroupName != null && !newGroupName.isEmpty()) {
                InheritanceNode newNode = InheritanceNode.builder(newGroupName).build();
                user.data().add(newNode);
            }

            luckPerms.getUserManager().saveUser(user);
        });
    }

    /**
     * Removes a player from a race group.
     */
    public CompletableFuture<Void> removePlayerFromGroup(UUID uuid, String groupName) {
        if (!isAvailable() || groupName == null || groupName.isEmpty())
            return CompletableFuture.completedFuture(null);

        return luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;
            InheritanceNode node = InheritanceNode.builder(groupName).build();
            user.data().remove(node);
            luckPerms.getUserManager().saveUser(user);
        });
    }
}
