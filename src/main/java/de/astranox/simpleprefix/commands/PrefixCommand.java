package de.astranox.simpleprefix.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.managers.ChatManager;
import de.astranox.simpleprefix.managers.ConfigManager;
import de.astranox.simpleprefix.managers.GroupManager;
import de.astranox.simpleprefix.managers.MigrationManager;
import de.astranox.simpleprefix.managers.TeamManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PrefixCommand {

    private final SimplePrefix plugin;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final TeamManager teamManager;
    private final ChatManager chatManager;
    private final MigrationManager migrationManager;

    private static final String PREFIX = "<gray>「<gradient:#FFA07A:#FF6B9D>SimplePrefix</gradient><gray>」";

    public PrefixCommand(SimplePrefix plugin, ConfigManager configManager, GroupManager groupManager,
                         TeamManager teamManager, ChatManager chatManager, MigrationManager migrationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.teamManager = teamManager;
        this.chatManager = chatManager;
        this.migrationManager = migrationManager;
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("sp")
                .requires(source -> source.getSender().hasPermission("simpleprefix.use"))
                .executes(this::showHelp)

                .then(Commands.literal("help")
                        .executes(this::showHelp))

                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.reload"))
                        .executes(this::handleReload))

                .then(Commands.literal("update")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.update"))
                        .executes(this::handleUpdateAll)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .executes(this::handleUpdatePlayer)))

                .then(Commands.literal("list")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.list"))
                        .executes(this::handleList))

                .then(Commands.literal("create")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.create"))
                        .then(Commands.argument("group", StringArgumentType.word())
                                .then(Commands.argument("prefix", StringArgumentType.greedyString())
                                        .executes(this::handleCreate))))

                .then(Commands.literal("set")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.set"))
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(this::suggestGroups)
                                .then(Commands.literal("prefix")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(this::handleSetPrefix)))
                                .then(Commands.literal("suffix")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(this::handleSetSuffix)))
                                .then(Commands.literal("priority")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 999))
                                                .executes(this::handleSetPriority)))
                                .then(Commands.literal("namecolor")
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(this::handleSetNameColor)))))

                .then(Commands.literal("clear")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.set"))
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(this::suggestGroups)
                                .then(Commands.literal("prefix")
                                        .executes(this::handleClearPrefix))
                                .then(Commands.literal("suffix")
                                        .executes(this::handleClearSuffix))
                                .then(Commands.literal("namecolor")
                                        .executes(this::handleClearNameColor))))

                .then(Commands.literal("delete")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.delete"))
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(this::suggestGroups)
                                .executes(this::handleDeleteGroup)))

                .then(Commands.literal("save")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.save"))
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests(this::suggestGroups)
                                .executes(this::handleSave)))

                .then(Commands.literal("format")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.format"))
                        .then(Commands.literal("chat")
                                .executes(this::handleFormatChatShow)
                                .then(Commands.literal("set")
                                        .then(Commands.argument("format", StringArgumentType.greedyString())
                                                .executes(this::handleFormatChatSet)))
                                .then(Commands.literal("toggle")
                                        .executes(this::handleFormatChatToggle)))
                        .then(Commands.literal("tab")
                                .executes(this::handleFormatTabShow)
                                .then(Commands.literal("set")
                                        .then(Commands.argument("format", StringArgumentType.greedyString())
                                                .executes(this::handleFormatTabSet)))
                                .then(Commands.literal("toggle")
                                        .executes(this::handleFormatTabToggle))))

                .then(Commands.literal("migrate")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.migrate"))
                        .then(Commands.literal("luckprefix")
                                .executes(this::handleMigrateLuckPrefix)))

                .then(Commands.literal("cleanup")
                        .requires(source -> source.getSender().hasPermission("simpleprefix.cleanup"))
                        .executes(this::handleCleanup));
    }

    private int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═══════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    " + PREFIX));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═══════════════════════════════════╝</gradient>"));
        sender.sendMessage("");
        sendPrefixMessage(sender, "<#FFA07A>/sp reload <gray>- Reload configs");
        sendPrefixMessage(sender, "<#FFA07A>/sp update [player] <gray>- Update teams");
        sendPrefixMessage(sender, "<#FFA07A>/sp list <gray>- Show all groups");
        sendPrefixMessage(sender, "<#FFA07A>/sp create <group> <prefix> <gray>- Create group");
        sendPrefixMessage(sender, "<#FFA07A>/sp delete <group> <gray>- Delete group");
        sendPrefixMessage(sender, "<#FFA07A>/sp migrate luckprefix <gray>- Migrate from LuckPrefix");
        sendPrefixMessage(sender, "<#FFA07A>/sp cleanup <gray>- Remove empty groups");
        sender.sendMessage("");
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500>▸ Group Management</gradient>"));
        sendPrefixMessage(sender, "<#FFA07A>/sp set <group> prefix <text> <gray>- Set prefix");
        sendPrefixMessage(sender, "<#FFA07A>/sp set <group> suffix <text> <gray>- Set suffix");
        sendPrefixMessage(sender, "<#FFA07A>/sp set <group> priority <num> <gray>- Set priority");
        sendPrefixMessage(sender, "<#FFA07A>/sp set <group> namecolor <color> <gray>- Set name color");
        sendPrefixMessage(sender, "<#FFA07A>/sp clear <group> prefix <gray>- Clear prefix");
        sendPrefixMessage(sender, "<#FFA07A>/sp clear <group> suffix <gray>- Clear suffix");
        sendPrefixMessage(sender, "<#FFA07A>/sp clear <group> namecolor <gray>- Clear name color");
        sendPrefixMessage(sender, "<#FFA07A>/sp save <group> <gray>- Save to config");
        sender.sendMessage("");
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500>▸ Format Management</gradient>"));
        sendPrefixMessage(sender, "<#FFA07A>/sp format chat <gray>- Show chat format");
        sendPrefixMessage(sender, "<#FFA07A>/sp format chat set <format> <gray>- Set format");
        sendPrefixMessage(sender, "<#FFA07A>/sp format chat toggle <gray>- Toggle chat");
        sendPrefixMessage(sender, "<#FFA07A>/sp format tab <gray>- Show tab format");
        sendPrefixMessage(sender, "<#FFA07A>/sp format tab set <format> <gray>- Set format");
        sendPrefixMessage(sender, "<#FFA07A>/sp format tab toggle <gray>- Toggle tab");
        return Command.SINGLE_SUCCESS;
    }

    private int handleCreate(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String groupName = context.getArgument("group", String.class);
        String prefix = context.getArgument("prefix", String.class);

        if (groupName.length() > 32) {
            sendPrefixMessage(sender, "<red>Group name too long! Maximum 32 characters.");
            return 0;
        }

        if (!groupName.matches("[a-zA-Z0-9_-]+")) {
            sendPrefixMessage(sender, "<red>Invalid group name! Only letters, numbers, - and _ allowed.");
            return 0;
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    <gradient:#FFA07A:#FF6B9D>Create Group</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═════════════════════════════════╝</gradient>"));
        sender.sendMessage("");

        boolean success = groupManager.createGroup(groupName, prefix, "", 999, null);

        if (!success) {
            sendPrefixMessage(sender, "<red>Failed to create group! Group may already exist.");
            return 0;
        }

        sendPrefixMessage(sender, "<green>Successfully created group: <yellow>" + groupName);
        sender.sendMessage("");
        sendPrefixMessage(sender, "<gray>Prefix: <reset>" + prefix);
        sendPrefixMessage(sender, "<gray>Priority: <yellow>999 <gray>(default)");
        sender.sendMessage("");

        if (plugin.isUsingLuckPerms()) {
            sendPrefixMessage(sender, "<green>✓ Group created in LuckPerms");
        }
        sendPrefixMessage(sender, "<green>✓ Group created in SimplePrefix");
        sender.sendMessage("");
        sendPrefixMessage(sender, "<gray>Use <yellow>/sp set " + groupName + " priority <num> <gray>to change priority");
        sendPrefixMessage(sender, "<gray>Use <yellow>/sp set " + groupName + " suffix <text> <gray>to add suffix");
        sendPrefixMessage(sender, "<gray>Use <yellow>/sp set " + groupName + " namecolor <color> <gray>to set name color");
        sender.sendMessage("");

        if (!plugin.isUsingLuckPerms()) {
            sendPrefixMessage(sender, "<yellow>ℹ Give players permission: <gold>simpleprefix.group." + groupName);
        }

        updateAllPlayers();

        return Command.SINGLE_SUCCESS;
    }

    private int handleDeleteGroup(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String groupName = context.getArgument("group", String.class);

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    <gradient:#FFA07A:#FF6B9D>Delete Group</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═════════════════════════════════╝</gradient>"));
        sender.sendMessage("");

        boolean success = groupManager.deleteGroupCompletely(groupName);

        if (!success) {
            sendPrefixMessage(sender, "<red>Failed to delete group! See console for details.");
            return 0;
        }

        sendPrefixMessage(sender, "<green>Successfully deleted group: <yellow>" + groupName);
        sender.sendMessage("");

        if (plugin.isUsingLuckPerms()) {
            sendPrefixMessage(sender, "<green>✓ Group deleted from LuckPerms");
        }
        sendPrefixMessage(sender, "<green>✓ Group deleted from SimplePrefix");

        updateAllPlayers();

        return Command.SINGLE_SUCCESS;
    }

    private int handleReload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        configManager.reloadConfig();
        groupManager.reloadGroups();
        updateAllPlayers();

        sendPrefixMessage(sender, "<green>Configs reloaded successfully!");
        return Command.SINGLE_SUCCESS;
    }

    private int handleUpdateAll(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        updateAllPlayers();

        sendPrefixMessage(sender, "<green>All players updated!");
        return Command.SINGLE_SUCCESS;
    }

    private int handleUpdatePlayer(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String playerName = context.getArgument("player", String.class);

        Player player = Bukkit.getPlayer(playerName);

        if (player == null) {
            sendPrefixMessage(sender, "<red>Player not found!");
            return 0;
        }

        teamManager.updatePlayerTeam(player);
        chatManager.updatePlayerListName(player);

        sendPrefixMessage(sender, "<green>Updated player: <yellow>" + player.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int handleList(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    <gradient:#FFA07A:#FF6B9D>Groups</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═════════════════════════════════╝</gradient>"));
        sender.sendMessage("");

        Map<String, GroupManager.GroupData> groups = groupManager.getAllGroups();

        for (Map.Entry<String, GroupManager.GroupData> entry : groups.entrySet()) {
            GroupManager.GroupData data = entry.getValue();
            sendPrefixMessage(sender, "<yellow>" + entry.getKey() + " <gray>(Priority: " + data.priority + ")");

            if (data.prefix != null && !data.prefix.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("  <gray>Prefix: <reset>" + data.prefix));
            }

            if (data.suffix != null && !data.suffix.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("  <gray>Suffix: <reset>" + data.suffix));
            }

            if (data.nameColor != null && !data.nameColor.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("  <gray>Name Color: <" + data.nameColor + ">" + data.nameColor));
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int handleSetPrefix(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        String value = context.getArgument("value", String.class);

        GroupManager.GroupData current = groupManager.getGroup(group);
        if (current == null) {
            sendPrefixMessage(context.getSource().getSender(), "<red>Group '" + group + "' not found in LuckPerms!");
            return 0;
        }

        String suffix = (current.suffix != null) ? current.suffix : "";
        int priority = current.priority;
        String nameColor = current.nameColor;

        groupManager.setGroup(group, value, suffix, priority, nameColor);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Prefix for group <yellow>" + group + " <green>set to: <reset>" + value);

        return Command.SINGLE_SUCCESS;
    }

    private int handleSetSuffix(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        String value = context.getArgument("value", String.class);

        GroupManager.GroupData current = groupManager.getGroup(group);
        if (current == null) {
            sendPrefixMessage(context.getSource().getSender(), "<red>Group '" + group + "' not found in LuckPerms!");
            return 0;
        }

        String prefix = (current.prefix != null) ? current.prefix : "";
        int priority = current.priority;
        String nameColor = current.nameColor;

        groupManager.setGroup(group, prefix, value, priority, nameColor);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Suffix for group <yellow>" + group + " <green>set to: <reset>" + value);

        return Command.SINGLE_SUCCESS;
    }

    private int handleSetPriority(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        int value = context.getArgument("value", Integer.class);

        GroupManager.GroupData current = groupManager.getGroup(group);
        if (current == null) {
            sendPrefixMessage(context.getSource().getSender(), "<red>Group '" + group + "' not found in LuckPerms!");
            return 0;
        }

        String prefix = (current.prefix != null) ? current.prefix : "";
        String suffix = (current.suffix != null) ? current.suffix : "";
        String nameColor = current.nameColor;

        groupManager.setGroup(group, prefix, suffix, value, nameColor);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Priority for group <yellow>" + group + " <green>set to: <yellow>" + value);

        return Command.SINGLE_SUCCESS;
    }

    private int handleSetNameColor(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        String value = context.getArgument("value", String.class);

        GroupManager.GroupData current = groupManager.getGroup(group);
        if (current == null) {
            sendPrefixMessage(context.getSource().getSender(), "<red>Group '" + group + "' not found in LuckPerms!");
            return 0;
        }

        String prefix = (current.prefix != null) ? current.prefix : "";
        String suffix = (current.suffix != null) ? current.suffix : "";
        int priority = current.priority;

        groupManager.setGroup(group, prefix, suffix, priority, value);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Name color for group <yellow>" + group + " <green>set to: <reset>" + value);

        return Command.SINGLE_SUCCESS;
    }

    private int handleClearPrefix(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);

        GroupManager.GroupData current = groupManager.getGroup(group);
        if (current == null) {
            sendPrefixMessage(context.getSource().getSender(), "<red>Group '" + group + "' not found in LuckPerms!");
            return 0;
        }

        String suffix = (current.suffix != null) ? current.suffix : "";
        int priority = current.priority;
        String nameColor = current.nameColor;

        groupManager.setGroup(group, "", suffix, priority, nameColor);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Prefix for group <yellow>" + group + " <green>cleared!");

        return Command.SINGLE_SUCCESS;
    }

    private int handleClearSuffix(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);

        GroupManager.GroupData current = groupManager.getGroup(group);
        if (current == null) {
            sendPrefixMessage(context.getSource().getSender(), "<red>Group '" + group + "' not found in LuckPerms!");
            return 0;
        }

        String prefix = (current.prefix != null) ? current.prefix : "";
        int priority = current.priority;
        String nameColor = current.nameColor;

        groupManager.setGroup(group, prefix, "", priority, nameColor);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Suffix for group <yellow>" + group + " <green>cleared!");

        return Command.SINGLE_SUCCESS;
    }

    private int handleClearNameColor(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);

        GroupManager.GroupData current = groupManager.getGroup(group);
        if (current == null) {
            sendPrefixMessage(context.getSource().getSender(), "<red>Group '" + group + "' not found in LuckPerms!");
            return 0;
        }

        String prefix = (current.prefix != null) ? current.prefix : "";
        String suffix = (current.suffix != null) ? current.suffix : "";
        int priority = current.priority;

        groupManager.setGroup(group, prefix, suffix, priority, null);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Name color for group <yellow>" + group + " <green>cleared!");

        return Command.SINGLE_SUCCESS;
    }

    private int handleDelete(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);

        groupManager.deleteGroup(group);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Group <yellow>" + group + " <green>deleted!");

        return Command.SINGLE_SUCCESS;
    }

    private int handleSave(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);

        groupManager.saveToConfig(group);
        sendPrefixMessage(context.getSource().getSender(), "<green>Group <yellow>" + group + " <green>saved to config!");

        return Command.SINGLE_SUCCESS;
    }

    private int handleFormatChatShow(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        String format = configManager.getChatFormat();
        boolean enabled = configManager.isChatFormatEnabled();

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    <gradient:#FFA07A:#FF6B9D>Chat Format</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═════════════════════════════════╝</gradient>"));
        sender.sendMessage("");
        sendPrefixMessage(sender, "<gray>Status: " + (enabled ? "<green>Enabled" : "<red>Disabled"));
        sendPrefixMessage(sender, "<gray>Format: <yellow>" + format);

        return Command.SINGLE_SUCCESS;
    }

    private int handleFormatChatSet(CommandContext<CommandSourceStack> context) {
        String format = context.getArgument("format", String.class);

        configManager.setChatFormat(format);
        sendPrefixMessage(context.getSource().getSender(), "<green>Chat format set to: <yellow>" + format);

        return Command.SINGLE_SUCCESS;
    }

    private int handleFormatChatToggle(CommandContext<CommandSourceStack> context) {
        boolean current = configManager.isChatFormatEnabled();
        configManager.setChatEnabled(!current);

        sendPrefixMessage(context.getSource().getSender(), "<green>Chat format " + (!current ? "<green>enabled" : "<red>disabled") + "!");

        return Command.SINGLE_SUCCESS;
    }

    private int handleFormatTabShow(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        String format = configManager.getTabFormat();
        boolean enabled = configManager.isTabFormatEnabled();

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    <gradient:#FFA07A:#FF6B9D>Tab Format</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═════════════════════════════════╝</gradient>"));
        sender.sendMessage("");
        sendPrefixMessage(sender, "<gray>Status: " + (enabled ? "<green>Enabled" : "<red>Disabled"));
        sendPrefixMessage(sender, "<gray>Format: <yellow>" + format);

        return Command.SINGLE_SUCCESS;
    }

    private int handleFormatTabSet(CommandContext<CommandSourceStack> context) {
        String format = context.getArgument("format", String.class);

        configManager.setTabFormat(format);
        updateAllPlayers();
        sendPrefixMessage(context.getSource().getSender(), "<green>Tab format set to: <yellow>" + format);

        return Command.SINGLE_SUCCESS;
    }

    private int handleFormatTabToggle(CommandContext<CommandSourceStack> context) {
        boolean current = configManager.isTabFormatEnabled();
        configManager.setTabEnabled(!current);
        updateAllPlayers();

        sendPrefixMessage(context.getSource().getSender(), "<green>Tab format " + (!current ? "<green>enabled" : "<red>disabled") + "!");

        return Command.SINGLE_SUCCESS;
    }

    private int handleMigrateLuckPrefix(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    <gradient:#FFA07A:#FF6B9D>LuckPrefix Migration</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═════════════════════════════════╝</gradient>"));
        sender.sendMessage("");
        sendPrefixMessage(sender, "<yellow>Starting migration from LuckPrefix...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MigrationManager.MigrationResult result = migrationManager.migrateLuckPrefix();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!result.success) {
                    sendPrefixMessage(sender, "<red>" + result.message);
                    return;
                }

                sendPrefixMessage(sender, "<green>" + result.message);
                sender.sendMessage("");

                if (!result.details.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500>▸ Migration Details</gradient>"));

                    for (Map.Entry<String, String> entry : result.details.entrySet()) {
                        String status = entry.getValue();
                        String color = status.startsWith("✓") ? "<green>" : "<gray>";
                        sendPrefixMessage(sender, color + "  • " + entry.getKey() + " <dark_gray>→ <gray>" + status);
                    }
                }

                sender.sendMessage("");
                sendPrefixMessage(sender, "<green>Migration complete! Updating all players...");

                updateAllPlayers();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    sendPrefixMessage(sender, "<green>All players updated successfully!");
                }, 20L);
            });
        });

        return Command.SINGLE_SUCCESS;
    }

    private int handleCleanup(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╔═════════════════════════════════╗</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("    <gradient:#FFA07A:#FF6B9D>Group Cleanup</gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FF6B9D:#C44569>╚═════════════════════════════════╝</gradient>"));
        sender.sendMessage("");
        sendPrefixMessage(sender, "<yellow>Cleaning up empty groups...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MigrationManager.MigrationResult result = migrationManager.cleanupEmptyGroups();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.migratedCount == 0) {
                    sendPrefixMessage(sender, "<green>" + result.message);
                    return;
                }

                sendPrefixMessage(sender, "<green>" + result.message);
                sender.sendMessage("");

                if (!result.details.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#FFD700:#FFA500>▸ Removed Groups</gradient>"));

                    for (String groupName : result.details.keySet()) {
                        sendPrefixMessage(sender, "<red>  • " + groupName);
                    }
                }

                sender.sendMessage("");
                sendPrefixMessage(sender, "<green>Cleanup complete! Updating all players...");

                updateAllPlayers();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    sendPrefixMessage(sender, "<green>All players updated successfully!");
                }, 20L);
            });
        });

        return Command.SINGLE_SUCCESS;
    }

    private void updateAllPlayers() {
        plugin.updateAllPlayers();
    }

    private void sendPrefixMessage(CommandSender sender, String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(PREFIX + " <gray>» " + message));
    }

    private CompletableFuture<Suggestions> suggestGroups(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Map<String, GroupManager.GroupData> groups = groupManager.getAllGroups();

        for (String groupName : groups.keySet()) {
            builder.suggest(groupName);
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestPlayers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            builder.suggest(player.getName());
        }

        return builder.buildFuture();
    }
}
