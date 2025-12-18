package de.astranox.simpleprefix.commands;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.managers.*;
import de.astranox.simpleprefix.update.UpdateChannel;
import de.astranox.simpleprefix.update.VersionInfo;
import de.astranox.simpleprefix.util.ComponentParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PrefixCommand implements CommandExecutor, TabCompleter {
    private final SimplePrefix plugin;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final TeamManager teamManager;
    private final TabChatManager chatManager;
    private final MigrationManager migrationManager;

    private final String prefix;

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public PrefixCommand(SimplePrefix plugin,
                         ConfigManager configManager,
                         GroupManager groupManager,
                         TeamManager teamManager,
                         TabChatManager chatManager,
                         MigrationManager migrationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.teamManager = teamManager;
        this.chatManager = chatManager;
        this.migrationManager = migrationManager;
        if (isModern()) {
            prefix = "<gray>「<gradient:#FFA07A:#FF6B9D>SimplePrefix</gradient><gray>」";
            return;
        }
        prefix = "&7「&6SimplePrefix&7」";
    }

    private static String join(String[] a, int start) {
        if (start >= a.length) return "";
        return String.join(" ", Arrays.copyOfRange(a, start, a.length));
    }

    private static List<String> filter(List<String> options, String token) {
        String t = token.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(t))
                .collect(Collectors.toList());
    }

    private void sendMM(CommandSender sender, String mini) {
        ComponentParser parser = new ComponentParser(plugin);
        sender.sendMessage(parser.parse(mini));
    }

    private void sendPrefix(CommandSender sender, String mini) {
        sendMM(sender, prefix + " » " + mini);
    }

    private void header(CommandSender sender, String title) {
        sendMM(sender, "&8╔═══════════════════════════════╗");
        sendMM(sender, " " + title);
        sendMM(sender, "&8╚═══════════════════════════════╝");
        sender.sendMessage("");
    }

    private void require(CommandSender sender, String perm) throws NoPermission {
        if (!sender.hasPermission(perm)) {
            sendPrefix(sender, "<red>No permission: <white>" + perm);
            throw new NoPermission();
        }
    }

    private void updateAllPlayers() {
        plugin.updateAllPlayers();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!sender.hasPermission("simpleprefix.use")) {
                sendPrefix(sender, "<red>No permission: <white>simpleprefix.use");
                return true;
            }

            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                showHelp(sender);
                return true;
            }

            String sub = args[0].toLowerCase(Locale.ROOT);

            switch (sub) {
                case "reload": {
                    require(sender, "simpleprefix.reload");
                    configManager.reloadConfig();
                    groupManager.reloadGroups();
                    updateAllPlayers();
                    sendPrefix(sender, "<green>Configs reloaded successfully!");
                    return true;
                }

                case "update": {
                    require(sender, "simpleprefix.update");

                    if (args.length >= 2 && args[1].equalsIgnoreCase("channel")) {
                        if (args.length >= 3) {
                            UpdateChannel channel = UpdateChannel.fromString(args[2]);
                            plugin.getUpdateChecker().setUpdateChannel(channel);
                            sendPrefix(sender, "<green>Update channel set to: <white>" + channel.getDisplayName());
                            sendPrefix(sender, "<gray>Run <white>/sp update check <gray>to check for updates");
                            return true;
                        }
                            UpdateChannel current = plugin.getUpdateChecker().getCurrentChannel();
                            sendPrefix(sender, "<yellow>Current update channel: <white>" + current.getDisplayName());
                            sendPrefix(sender, "<gray>Available channels:");
                            sendPrefix(sender, "<white>• dev <gray>- All updates (dev, snapshot, stable)");
                            sendPrefix(sender, "<white>• snapshot <gray>- Snapshot + Stable releases");
                            sendPrefix(sender, "<white>• stable <gray>- Only stable releases");
                        return true;
                    }

                    if (args.length >= 2 && args[1].equalsIgnoreCase("install")) {
                        if (!plugin.getUpdateChecker().isUpdateAvailable()) {
                            sendPrefix(sender, "<red>No update available!");
                            return true;
                        }

                        boolean hotReload = args.length >= 3 && args[2].equalsIgnoreCase("--hot");

                        if (hotReload) {
                            header(sender, "Hot-Reload Update");
                            sendPrefix(sender, "<red><bold>⚠ WARNING: Experimental feature!");
                            sendPrefix(sender, "<yellow>Downloading and hot-reloading v" +
                                    plugin.getUpdateChecker().getLatestVersionInfo().getVersion());
                        } else {
                            header(sender, "Update Installer");
                            sendPrefix(sender, "<yellow>Downloading update v" +
                                    plugin.getUpdateChecker().getLatestVersionInfo().getVersion() + "...");
                        }

                        plugin.getUpdateChecker().installUpdate(hotReload).thenAccept(result -> {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (result.isSuccess()) {
                                    sendPrefix(sender, "<green>✓ " + result.getMessage());
                                    if (!hotReload) {
                                        sendPrefix(sender, "<gray>Old version backed up as .old");
                                        sendPrefix(sender, "<gold><bold>Restart the server to apply!");
                                    }
                                } else {
                                    sendPrefix(sender, "<red>✗ " + result.getMessage());
                                }
                            });
                        });
                        return true;
                    }

                    if (args.length >= 2 && args[1].equalsIgnoreCase("info")) {
                        if (!plugin.getUpdateChecker().isUpdateAvailable()) {
                            sendPrefix(sender, "<red>No update available!");
                            sendPrefix(sender, "<gray>Current version: <white>" + plugin.getDescription().getVersion());
                            return true;
                        }

                        VersionInfo info = plugin.getUpdateChecker().getLatestVersionInfo();
                        header(sender, "Update Information");
                        sendPrefix(sender, "<yellow>Current: <gray>" + plugin.getDescription().getVersion());
                        sendPrefix(sender, "<yellow>Latest: <green>" + info.getVersion());
                        sendPrefix(sender, "");
                        sendPrefix(sender, "<yellow>Download: <aqua>" + info.getDownloadUrl());
                        sendPrefix(sender, "");
                        sendPrefix(sender, "<gold>Run <white>/sp update install <gold>to download");
                        return true;
                    }

                    if (args.length >= 2 && args[1].equalsIgnoreCase("check")) {
                        sendPrefix(sender, "<yellow>Checking for updates...");
                        plugin.getUpdateChecker().resetNotifications();
                        plugin.getUpdateChecker().checkForUpdates();

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (plugin.getUpdateChecker().isUpdateAvailable()) {
                                sendPrefix(sender, "<green>Update available: v" +
                                        plugin.getUpdateChecker().getLatestVersionInfo().getVersion());
                                sendPrefix(sender, "<gold>Run <white>/sp update install <gold>to download");
                                return;
                            }
                            sendPrefix(sender, "<green>You are running the latest version!");
                        }, 40L);
                        return true;
                    }

                    if (args.length == 1) {
                        updateAllPlayers();
                        sendPrefix(sender, "<green>All players updated!");
                        return true;
                    }

                    String playerName = args[1];
                    Player p = Bukkit.getPlayerExact(playerName);
                    if (p == null) {
                        sendPrefix(sender, "<red>Player not found!");
                        return true;
                    }

                    teamManager.updatePlayer(p);
                    sendPrefix(sender, "<green>Updated player: " + p.getName());
                    return true;
                }

                case "list": {
                    require(sender, "simpleprefix.list");
                    header(sender, "Groups");
                    Map<String, GroupManager.GroupData> groups = groupManager.getAllGroups();
                    for (Map.Entry<String, GroupManager.GroupData> e : groups.entrySet()) {
                        GroupManager.GroupData d = e.getValue();
                        sendPrefix(sender, "<yellow>" + e.getKey() + " <gray>(Priority: <white>" + d.priority + "<gray>)");
                        if (d.prefix != null && !d.prefix.isEmpty()) {
                            sendMM(sender, "  Prefix: " + d.prefix);
                        }
                        if (d.suffix != null && !d.suffix.isEmpty()) {
                            sendMM(sender, "  Suffix: " + d.suffix);
                        }
                        if (d.nameColor != null && !d.nameColor.isEmpty()) {
                            sendMM(sender, "  Name Color: <" + d.nameColor + ">" + d.nameColor);
                        }
                    }
                    return true;
                }

                case "create": {
                    require(sender, "simpleprefix.create");
                    if (args.length < 3) {
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " create <group> <prefix...>");
                        return true;
                    }
                    String groupName = args[1];
                    String prefix = join(args, 2);

                    if (groupName.length() > 32) {
                        sendPrefix(sender, "<red>Group name too long! Maximum 32 characters.");
                        return true;
                    }
                    if (!groupName.matches("[a-zA-Z0-9_-]+")) {
                        sendPrefix(sender, "<red>Invalid group name! Only letters, numbers, - and _ allowed.");
                        return true;
                    }

                    header(sender, "Create Group");
                    boolean success = groupManager.createGroup(groupName, prefix, "", 999, null);
                    if (!success) {
                        sendPrefix(sender, "<red>Failed to create group! Group may already exist.");
                        return true;
                    }

                    sendPrefix(sender, "<green>Successfully created group: <white>" + groupName);
                    sender.sendMessage("");
                    sendPrefix(sender, "<gray>Prefix: <white>" + prefix);
                    sendPrefix(sender, "<gray>Priority: <white>999 <gray>(default)");
                    sender.sendMessage("");

                    if (plugin.isUsingLuckPerms()) {
                        sendPrefix(sender, "<green>✓ Group created in LuckPerms");
                    }
                    sendPrefix(sender, "<green>✓ Group created in SimplePrefix");
                    sender.sendMessage("");
                    sendPrefix(sender, "<gray>Use <white>/" + label + " set " + groupName + " priority <yellow><value><gray> to change priority");
                    sendPrefix(sender, "<gray>Use <white>/" + label + " set " + groupName + " suffix <yellow><value><gray> to add suffix");
                    sendPrefix(sender, "<gray>Use <white>/" + label + " set " + groupName + " namecolor <yellow><value><gray> to set name color");
                    sender.sendMessage("");

                    if (!plugin.isUsingLuckPerms()) {
                        sendPrefix(sender, "<yellow>ℹ Give players permission: <white>simpleprefix.group." + groupName);
                    }

                    updateAllPlayers();
                    return true;
                }

                case "delete": {
                    require(sender, "simpleprefix.delete");
                    if (args.length < 2) {
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " delete <group>");
                        return true;
                    }
                    String groupName = args[1];
                    header(sender, "Delete Group");
                    boolean success = groupManager.deleteGroupCompletely(groupName);
                    if (!success) {
                        sendPrefix(sender, "<red>Failed to delete group! See console for details.");
                        return true;
                    }
                    sendPrefix(sender, "<green>Successfully deleted group: <white>" + groupName);
                    if (plugin.isUsingLuckPerms()) {
                        sendPrefix(sender, "<green>✓ Group deleted from LuckPerms");
                    }
                    sendPrefix(sender, "<green>✓ Group deleted from SimplePrefix");
                    updateAllPlayers();
                    return true;
                }

                case "set": {
                    require(sender, "simpleprefix.set");
                    if (args.length < 4) {
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " set <group> <prefix|suffix|priority|namecolor> <value>");
                        return true;
                    }
                    String group = args[1];
                    String field = args[2].toLowerCase(Locale.ROOT);
                    String value = join(args, 3);

                    GroupManager.GroupData current = groupManager.getGroup(group);
                    if (current == null) {
                        sendPrefix(sender, "<red>Group '<white>" + group + "</white>' not found in LuckPerms!");
                        return true;
                    }

                    String prefix = current.prefix != null ? current.prefix : "";
                    String suffix = current.suffix != null ? current.suffix : "";
                    int priority = current.priority;
                    String nameColor = current.nameColor;

                    switch (field) {
                        case "prefix":
                            prefix = value;
                            groupManager.setGroup(group, prefix, suffix, priority, nameColor);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Prefix for group <white>" + group + "</white> set to: <white>" + value);
                            return true;
                        case "suffix":
                            suffix = value;
                            groupManager.setGroup(group, prefix, suffix, priority, nameColor);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Suffix for group <white>" + group + "</white> set to: <white>" + value);
                            return true;
                        case "priority":
                            try {
                                priority = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                sendPrefix(sender, "<red>Priority must be a number between 0 and 999.");
                                return true;
                            }
                            if (priority < 0 || priority > 999) {
                                sendPrefix(sender, "<red>Priority must be a number between 0 and 999.");
                                return true;
                            }
                            groupManager.setGroup(group, prefix, suffix, priority, nameColor);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Priority for group <white>" + group + "</white> set to: <white>" + priority);
                            return true;
                        case "namecolor":
                            nameColor = value;
                            groupManager.setGroup(group, prefix, suffix, priority, nameColor);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Name color for group <white>" + group + "</white> set to: <white>" + value);
                            return true;
                        default:
                            sendPrefix(sender, "<red>Unknown field: <white>" + field + "</white> (use prefix|suffix|priority|namecolor)");
                            return true;
                    }
                }

                case "clear": {
                    require(sender, "simpleprefix.set");
                    if (args.length < 3) {
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " clear <group> <prefix|suffix|namecolor>");
                        return true;
                    }
                    String group = args[1];
                    String field = args[2].toLowerCase(Locale.ROOT);

                    GroupManager.GroupData current = groupManager.getGroup(group);
                    if (current == null) {
                        sendPrefix(sender, "<red>Group '<white>" + group + "</white>' not found in LuckPerms!");
                        return true;
                    }

                    String prefix = current.prefix != null ? current.prefix : "";
                    String suffix = current.suffix != null ? current.suffix : "";
                    int priority = current.priority;
                    String nameColor = current.nameColor;

                    switch (field) {
                        case "prefix":
                            groupManager.setGroup(group, "", suffix, priority, nameColor);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Prefix for group <white>" + group + "</white> cleared!");
                            return true;
                        case "suffix":
                            groupManager.setGroup(group, prefix, "", priority, nameColor);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Suffix for group <white>" + group + "</white> cleared!");
                            return true;
                        case "namecolor":
                            groupManager.setGroup(group, prefix, suffix, priority, null);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Name color for group <white>" + group + "</white> cleared!");
                            return true;
                        default:
                            sendPrefix(sender, "<red>Unknown field: <white>" + field + "</white> (use prefix|suffix|namecolor)");
                            return true;
                    }
                }

                case "save": {
                    require(sender, "simpleprefix.save");
                    if (args.length < 2) {
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " save <group>");
                        return true;
                    }
                    String group = args[1];
                    groupManager.saveToConfig(group);
                    sendPrefix(sender, "<green>Group <white>" + group + "</white> saved to config!");
                    return true;
                }

                case "format": {
                    require(sender, "simpleprefix.format");
                    if (args.length < 2) {
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " format <chat|tab> [set <format...>|toggle]");
                        return true;
                    }
                    String target = args[1].toLowerCase(Locale.ROOT);
                    if (target.equals("chat")) {
                        if (args.length == 2) {
                            header(sender, "Chat Format");
                            String format = configManager.getChatFormat();
                            boolean enabled = configManager.isChatFormatEnabled();
                            sendPrefix(sender, "<gray>Status: <white>" + (enabled ? "Enabled" : "Disabled"));
                            sendPrefix(sender, "<gray>Format: <white>" + format);
                            return true;
                        }
                        if (args.length >= 3 && args[2].equalsIgnoreCase("set")) {
                            if (args.length < 4) {
                                sendPrefix(sender, "<red>Usage: <white>/" + label + " format chat set <format...>");
                                return true;
                            }
                            String format = join(args, 3);
                            configManager.setChatFormat(format);
                            sendPrefix(sender, "<green>Chat format set to: <white>" + format);
                            return true;
                        }
                        if (args.length >= 3 && args[2].equalsIgnoreCase("toggle")) {
                            boolean current = configManager.isChatFormatEnabled();
                            configManager.setChatEnabled(!current);
                            sendPrefix(sender, "<green>Chat format " + (!current ? "enabled" : "disabled") + "!");
                            return true;
                        }
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " format chat [set <format...>|toggle]");
                        return true;
                    } else if (target.equals("tab")) {
                        if (args.length == 2) {
                            header(sender, "Tab Format");
                            String format = configManager.getTabFormat();
                            boolean enabled = configManager.isTabFormatEnabled();
                            sendPrefix(sender, "<gray>Status: <white>" + (enabled ? "Enabled" : "Disabled"));
                            sendPrefix(sender, "<gray>Format: <white>" + format);
                            return true;
                        }
                        if (args.length >= 3 && args[2].equalsIgnoreCase("set")) {
                            if (args.length < 4) {
                                sendPrefix(sender, "<red>Usage: <white>/" + label + " format tab set <format...>");
                                return true;
                            }
                            String format = join(args, 3);
                            configManager.setTabFormat(format);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Tab format set to: <white>" + format);
                            return true;
                        }
                        if (args.length >= 3 && args[2].equalsIgnoreCase("toggle")) {
                            boolean current = configManager.isTabFormatEnabled();
                            configManager.setTabEnabled(!current);
                            updateAllPlayers();
                            sendPrefix(sender, "<green>Tab format " + (!current ? "enabled" : "disabled") + "!");
                            return true;
                        }
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " format tab [set <format...>|toggle]");
                        return true;
                    } else {
                        sendPrefix(sender, "<red>Unknown format target: <white>" + target + "</white> (use chat|tab)");
                        return true;
                    }
                }

                case "migrate": {
                    require(sender, "simpleprefix.migrate");
                    if (args.length < 2 || !args[1].equalsIgnoreCase("luckprefix")) {
                        sendPrefix(sender, "<red>Usage: <white>/" + label + " migrate luckprefix");
                        return true;
                    }
                    header(sender, "LuckPrefix Migration");
                    sendPrefix(sender, "<gray>Starting migration from LuckPrefix...");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        MigrationManager.MigrationResult result = migrationManager.migrateLuckPrefix();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!result.success) {
                                sendPrefix(sender, "<red>" + result.message);
                                return;
                            }
                            sendPrefix(sender, "<green>" + result.message);
                            sender.sendMessage("");
                            if (!result.details.isEmpty()) {
                                sendMM(sender, "▸ Migration Details");
                                for (Map.Entry<String, String> e : result.details.entrySet()) {
                                    String status = e.getValue();
                                    String color = status.startsWith("✓") ? "<green>" : "<yellow>";
                                    sendPrefix(sender, color + " • " + e.getKey() + " → " + status);
                                }
                            }
                            sender.sendMessage("");
                            sendPrefix(sender, "<gray>Migration complete! Updating all players...");
                            updateAllPlayers();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                sendPrefix(sender, "<green>All players updated successfully!");
                            }, 20L);
                        });
                    });
                    return true;
                }

                case "cleanup": {
                    require(sender, "simpleprefix.cleanup");
                    header(sender, "Group Cleanup");
                    sendPrefix(sender, "<gray>Cleaning up empty groups...");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        MigrationManager.MigrationResult result = migrationManager.cleanupEmptyGroups();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (result.migratedCount == 0) {
                                sendPrefix(sender, "<yellow>" + result.message);
                                return;
                            }
                            sendPrefix(sender, "<green>" + result.message);
                            sender.sendMessage("");
                            if (!result.details.isEmpty()) {
                                sendMM(sender, "▸ Removed Groups");
                                for (String groupName : result.details.keySet()) {
                                    sendPrefix(sender, "<yellow> • " + groupName);
                                }
                            }
                            sender.sendMessage("");
                            sendPrefix(sender, "<gray>Cleanup complete! Updating all players...");
                            updateAllPlayers();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                sendPrefix(sender, "<green>All players updated successfully!");
                            }, 20L);
                        });
                    });
                    return true;
                }

                default:
                    showHelp(sender);
                    return true;
            }
        } catch (NoPermission ignored) {
            return true;
        } catch (Throwable t) {
            sendPrefix(sender, "<red>An internal error occurred. See console.");
            t.printStackTrace();
            return true;
        }
    }

    private void showHelp(CommandSender sender) {
        sendMM(sender, "&8╔═════════════════════════════════╗");
        sendMM(sender, " " + prefix);
        sendMM(sender, "&8╚═════════════════════════════════╝");
        sender.sendMessage("");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " reload - Reload configs");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " update [player] - Update teams");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " list - Show all groups");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " create - Create group");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " delete - Delete group");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " migrate luckprefix - Migrate from LuckPrefix");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " cleanup - Remove empty groups");
        sender.sendMessage("");
        sendMM(sender, "▸ Group Management");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " set prefix - Set prefix");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " set suffix - Set suffix");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " set priority - Set priority");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " set namecolor - Set name color");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " clear prefix - Clear prefix");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " clear suffix - Clear suffix");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " clear namecolor - Clear name color");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " save - Save to config");
        sender.sendMessage("");
        sendMM(sender, "▸ Format Management");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " format chat - Show chat format");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " format chat set - Set format");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " format chat toggle - Toggle chat");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " format tab - Show tab format");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " format tab set - Set format");
        sendPrefix(sender, "<#FFA07A>/" + "sp" + " format tab toggle - Toggle tab");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> empty = Collections.emptyList();
        if (!sender.hasPermission("simpleprefix.use")) return empty;

        if (args.length == 1) {
            return filter(Arrays.asList("help", "reload", "update", "list", "create", "set", "clear", "delete", "save", "format", "migrate", "cleanup"), args[0]);
        }

        String a0 = args[0].toLowerCase(Locale.ROOT);

        switch (a0) {
            case "update":
                if (args.length == 2) {
                    List<String> opts = new ArrayList<>();
                    opts.add("install");
                    opts.add("info");
                    opts.add("check");
                    opts.add("channel");
                    opts.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()));
                    return filter(opts, args[1]);
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("channel")) {
                    return filter(Arrays.asList("dev", "snapshot", "stable"), args[2]);
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("install")) {
                    return filter(Collections.singletonList("--hot"), args[2]);
                }
                return empty;

            case "create":

                return empty;

            case "set":
                if (args.length == 2) {
                    return filter(new ArrayList<>(groupManager.getAllGroups().keySet()), args[1]);
                }
                if (args.length == 3) {
                    return filter(Arrays.asList("prefix", "suffix", "priority", "namecolor"), args[2]);
                }
                return empty;

            case "clear":
                if (args.length == 2) {
                    return filter(new ArrayList<>(groupManager.getAllGroups().keySet()), args[1]);
                }
                if (args.length == 3) {
                    return filter(Arrays.asList("prefix", "suffix", "namecolor"), args[2]);
                }
                return empty;

            case "delete":
            case "save":
                if (args.length == 2) {
                    return filter(new ArrayList<>(groupManager.getAllGroups().keySet()), args[1]);
                }
                return empty;

            case "format":
                if (args.length == 2) {
                    return filter(Arrays.asList("chat", "tab"), args[1]);
                }
                if (args.length == 3) {
                    return filter(Arrays.asList("set", "toggle"), args[2]);
                }
                return empty;

            case "migrate":
                if (args.length == 2) {
                    return filter(Collections.singletonList("luckprefix"), args[1]);
                }
                return empty;

            default:
                return empty;
        }
    }

    private boolean isModern() {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);

        return major > 1 || (major == 1 && minor >= 14);
    }

    private static class NoPermission extends Exception {
    }
}