package com.fantasyraces.commands;

import com.fantasyraces.FantasyRaces;
import com.fantasyraces.managers.AttributeManager;
import com.fantasyraces.models.Race;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RaceCommand implements CommandExecutor, TabCompleter {

    private final FantasyRaces plugin;

    public RaceCommand(FantasyRaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "select" -> handleSelect(sender, args);
            case "info"   -> handleInfo(sender, args);
            case "list"   -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "set"    -> handleSet(sender, args);
            case "reset"  -> handleReset(sender, args);
            case "who"    -> handleWho(sender, args);
            default       -> sendHelp(sender);
        }

        return true;
    }

    // ── /race select <race> ────────────────────────────────────────────────

    private void handleSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can select a race.");
            return;
        }

        if (!player.hasPermission("fantasyraces.select")) {
            player.sendMessage(plugin.prefix() + plugin.msg("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.prefix() + "&eUsage: /race select <race>");
            handleList(player);
            return;
        }

        String raceId = args[1].toLowerCase();
        Race race = plugin.getRaceManager().getRace(raceId);

        if (race == null) {
            player.sendMessage(plugin.prefix() + plugin.msg("unknown-race", "{race}", raceId));
            return;
        }

        UUID uuid = player.getUniqueId();
        String currentRaceId = plugin.getPlayerDataManager().getPlayerRace(uuid);

        if (raceId.equals(currentRaceId)) {
            player.sendMessage(plugin.prefix() + plugin.msg("already-selected",
                    "{race}", plugin.colorize(race.getDisplayName())));
            return;
        }

        boolean allowReselect = plugin.getConfig().getBoolean("settings.allow-reselect", true);
        if (!allowReselect && currentRaceId != null && !player.hasPermission("fantasyraces.admin")) {
            player.sendMessage(plugin.prefix() + "&cRace changes are not allowed.");
            return;
        }

        if (plugin.getPlayerDataManager().isOnCooldown(uuid) && !player.hasPermission("fantasyraces.admin")) {
            long remaining = plugin.getPlayerDataManager().getRemainingCooldown(uuid);
            player.sendMessage(plugin.prefix() + plugin.msg("cooldown", "{time}", formatTime(remaining)));
            return;
        }

        // Remove old race
        Race oldRace = plugin.getRaceManager().getRace(currentRaceId);
        if (oldRace != null) {
            plugin.getConditionManager().removeRaceEffects(player, oldRace);
        }
        AttributeManager.removeRaceModifiers(player);

        // Apply new race
        plugin.getPlayerDataManager().setPlayerRace(uuid, raceId);
        AttributeManager.applyRace(player, race);

        // LuckPerms
        String oldGroup = (oldRace != null) ? oldRace.getLuckPermsGroup() : null;
        String newGroup = race.getLuckPermsGroup();
        plugin.getLuckPermsManager().updatePlayerGroup(uuid, oldGroup, newGroup);

        player.sendMessage(plugin.prefix() + plugin.msg("selected",
                "{race}", plugin.colorize(race.getDisplayName())));

        // Broadcast
        if (plugin.getConfig().getBoolean("settings.broadcast-on-select", true)) {
            String broadcast = plugin.prefix() + plugin.msg("broadcast-select",
                    "{player}", player.getName(),
                    "{race}", plugin.colorize(race.getDisplayName()));
            Bukkit.broadcastMessage(broadcast);
        }
    }

    // ── /race info [race] ──────────────────────────────────────────────────

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fantasyraces.info")) {
            sender.sendMessage(plugin.prefix() + plugin.msg("no-permission"));
            return;
        }

        Race race;
        if (args.length >= 2) {
            race = plugin.getRaceManager().getRace(args[1]);
            if (race == null) {
                sender.sendMessage(plugin.prefix() + plugin.msg("unknown-race", "{race}", args[1]));
                return;
            }
        } else if (sender instanceof Player player) {
            String raceId = plugin.getPlayerDataManager().getPlayerRace(player.getUniqueId());
            if (raceId == null) {
                sender.sendMessage(plugin.prefix() + "&cYou have no race. Use &f/race select <race>&c.");
                return;
            }
            race = plugin.getRaceManager().getRace(raceId);
        } else {
            sender.sendMessage("Usage: /race info <race>");
            return;
        }

        sender.sendMessage(plugin.colorize(plugin.msg("race-info-header", "{race}", race.getDisplayName())));
        sender.sendMessage(plugin.colorize("&7" + race.getDescription()));
        sender.sendMessage(plugin.colorize("&eStats:"));
        sender.sendMessage(plugin.colorize("  &7Max Health Bonus: &f" + (race.getMaxHealthBonus() >= 0 ? "+" : "") + race.getMaxHealthBonus() + " hearts"));
        sender.sendMessage(plugin.colorize("  &7Speed Modifier:   &f" + (race.getSpeedModifier() >= 0 ? "+" : "") + String.format("%.0f%%", race.getSpeedModifier() * 100)));
        sender.sendMessage(plugin.colorize("  &7Damage Bonus:     &f" + (race.getDamageModifier() >= 0 ? "+" : "") + String.format("%.0f%%", race.getDamageModifier() * 100)));
        sender.sendMessage(plugin.colorize("  &7Knockback Res:    &f" + String.format("%.0f%%", race.getKnockbackResistance() * 100)));
        sender.sendMessage(plugin.colorize("  &7Scale:            &f" + race.getScale() + "x &8(1.0 = normal)"));

        if (!race.getPassives().isEmpty()) {
            sender.sendMessage(plugin.colorize("&ePassive Abilities:"));
            race.getPassives().forEach(p -> sender.sendMessage(plugin.colorize("  &7- " + formatAbility(p))));
        }

        if (!race.getConditions().isEmpty()) {
            sender.sendMessage(plugin.colorize("&eConditions:"));
            race.getConditions().forEach(c -> {
                String triggers = c.getTriggers().stream()
                        .map(Enum::name)
                        .reduce((a, b) -> a + " + " + b).orElse("?");
                String effectStr = c.getEffect() != null ? formatAbility(c.getEffect()) : "";
                String specialStr = c.hasSpecialEffect() ? c.getSpecialEffect().name() : "";
                String combined = effectStr.isEmpty() ? specialStr : (specialStr.isEmpty() ? effectStr : effectStr + " + " + specialStr);
                sender.sendMessage(plugin.colorize(
                        "  &7- &f" + triggers + (c.getYValue() != 0 ? " " + (int)c.getYValue() : "")
                                + " &7→ " + combined));
            });
        }

        if (race.getLuckPermsGroup() != null) {
            sender.sendMessage(plugin.colorize(plugin.msg("race-info-lp-group",
                    "{group}", race.getLuckPermsGroup())));
        }
    }

    // ── /race list ─────────────────────────────────────────────────────────

    private void handleList(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&6Available Races:"));
        for (Race race : plugin.getRaceManager().getAllRaces()) {
            String playerRace = (sender instanceof Player p)
                    ? plugin.getPlayerDataManager().getPlayerRace(p.getUniqueId())
                    : null;
            boolean current = race.getId().equals(playerRace);
            sender.sendMessage(plugin.colorize("  " + (current ? "&a✔ " : "&7- ") +
                    race.getDisplayName() + (current ? " &a(current)" : "") +
                    " &8- &7" + race.getDescription()));
        }
    }

    // ── /race reload ───────────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("fantasyraces.reload")) {
            sender.sendMessage(plugin.prefix() + plugin.msg("no-permission"));
            return;
        }
        plugin.reload();
        sender.sendMessage(plugin.prefix() + plugin.msg("reload-success"));
    }

    // ── /race set <player> <race> ──────────────────────────────────────────

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fantasyraces.admin")) {
            sender.sendMessage(plugin.prefix() + plugin.msg("no-permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&eUsage: /race set <player> <race>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + "&cPlayer not found: " + args[1]);
            return;
        }

        String raceId = args[2].toLowerCase();
        Race race = plugin.getRaceManager().getRace(raceId);
        if (race == null) {
            sender.sendMessage(plugin.prefix() + plugin.msg("unknown-race", "{race}", raceId));
            return;
        }

        UUID uuid = target.getUniqueId();
        String oldRaceId = plugin.getPlayerDataManager().getPlayerRace(uuid);
        Race oldRace = plugin.getRaceManager().getRace(oldRaceId);

        if (oldRace != null) plugin.getConditionManager().removeRaceEffects(target, oldRace);
        AttributeManager.removeRaceModifiers(target);

        plugin.getPlayerDataManager().setPlayerRace(uuid, raceId);
        AttributeManager.applyRace(target, race);

        String oldGroup = (oldRace != null) ? oldRace.getLuckPermsGroup() : null;
        plugin.getLuckPermsManager().updatePlayerGroup(uuid, oldGroup, race.getLuckPermsGroup());

        sender.sendMessage(plugin.prefix() + plugin.msg("set-success",
                "{player}", target.getName(), "{race}", plugin.colorize(race.getDisplayName())));
        target.sendMessage(plugin.prefix() + plugin.msg("selected",
                "{race}", plugin.colorize(race.getDisplayName())));
    }

    // ── /race reset <player> ───────────────────────────────────────────────

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fantasyraces.admin")) {
            sender.sendMessage(plugin.prefix() + plugin.msg("no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&eUsage: /race reset <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + "&cPlayer not found: " + args[1]);
            return;
        }

        UUID uuid = target.getUniqueId();
        String oldRaceId = plugin.getPlayerDataManager().getPlayerRace(uuid);
        Race oldRace = plugin.getRaceManager().getRace(oldRaceId);

        if (oldRace != null) {
            plugin.getConditionManager().removeRaceEffects(target, oldRace);
            plugin.getLuckPermsManager().removePlayerFromGroup(uuid, oldRace.getLuckPermsGroup());
        }
        AttributeManager.removeRaceModifiers(target);
        plugin.getPlayerDataManager().clearPlayerRace(uuid);

        sender.sendMessage(plugin.prefix() + plugin.msg("reset-success", "{player}", target.getName()));
    }

    // ── /race who <player> ─────────────────────────────────────────────────

    private void handleWho(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fantasyraces.admin")) {
            sender.sendMessage(plugin.prefix() + plugin.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&eUsage: /race who <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + "&cPlayer not found: " + args[1]);
            return;
        }
        String raceId = plugin.getPlayerDataManager().getPlayerRace(target.getUniqueId());
        Race race = plugin.getRaceManager().getRace(raceId);
        String name = (race != null) ? plugin.colorize(race.getDisplayName()) : "&7None";
        sender.sendMessage(plugin.prefix() + "&f" + target.getName() + "&7 is: " + name);
    }

    // ── Help ───────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&6FantasyRaces Commands:"));
        sender.sendMessage(plugin.colorize("  &f/race select <race> &7- Choose your race"));
        sender.sendMessage(plugin.colorize("  &f/race info [race]   &7- View race info"));
        sender.sendMessage(plugin.colorize("  &f/race list          &7- List all races"));
        if (sender.hasPermission("fantasyraces.admin")) {
            sender.sendMessage(plugin.colorize("  &f/race set <player> <race> &7- Admin: set race"));
            sender.sendMessage(plugin.colorize("  &f/race reset <player>      &7- Admin: clear race"));
            sender.sendMessage(plugin.colorize("  &f/race who <player>        &7- Admin: view race"));
            sender.sendMessage(plugin.colorize("  &f/race reload              &7- Reload config"));
        }
    }

    // ── Tab Completion ─────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("select", "info", "list"));
            if (sender.hasPermission("fantasyraces.admin")) {
                subs.addAll(Arrays.asList("set", "reset", "reload", "who"));
            }
            return filter(subs, args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "select", "info" -> filter(
                        plugin.getRaceManager().getAllRaces().stream()
                                .map(Race::getId).collect(Collectors.toList()), args[1]);
                case "set", "reset", "who" -> filter(
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName).collect(Collectors.toList()), args[1]);
                default -> result;
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(plugin.getRaceManager().getAllRaces().stream()
                    .map(Race::getId).collect(Collectors.toList()), args[2]);
        }

        return result;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private List<String> filter(List<String> options, String partial) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private String formatAbility(com.fantasyraces.models.PassiveAbility ability) {
        String level = ability.getAmplifier() > 0 ? " " + toRoman(ability.getAmplifier() + 1) : "";
        return ability.getType().name().replace("_", " ") + level;
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n);
        };
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
}
