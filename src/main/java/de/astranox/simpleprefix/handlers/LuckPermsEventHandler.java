package de.astranox.simpleprefix.handlers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.managers.ChatManager;
import de.astranox.simpleprefix.managers.ConfigManager;
import de.astranox.simpleprefix.managers.GroupManager;
import de.astranox.simpleprefix.managers.TeamManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LuckPermsEventHandler {

    private final SimplePrefix plugin;
    private final LuckPerms luckPerms;
    private final TeamManager teamManager;
    private final ChatManager chatManager;
    private final ConfigManager configManager;
    private final GroupManager groupManager;

    public LuckPermsEventHandler(SimplePrefix plugin, LuckPerms luckPerms, TeamManager teamManager,
                                 ChatManager chatManager, ConfigManager configManager, GroupManager groupManager) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.teamManager = teamManager;
        this.chatManager = chatManager;
        this.configManager = configManager;
        this.groupManager = groupManager;
    }

    public void register() {
        EventBus eventBus = luckPerms.getEventBus();

        eventBus.subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        eventBus.subscribe(plugin, NodeAddEvent.class, this::onNodeAdd);
        eventBus.subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove);
        eventBus.subscribe(plugin, GroupDataRecalculateEvent.class, this::onGroupDataRecalculate);

        plugin.getLogger().info("LuckPerms event listeners registered!");
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        if (!configManager.isAutoUpdateEnabled()) {
            return;
        }

        UUID uuid = event.getUser().getUniqueId();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                return;
            }

            teamManager.updatePlayerTeam(player);
            chatManager.updatePlayerListName(player);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Auto-updated " + player.getName() + " (Data recalculated)");
            }
        });
    }

    private void onNodeAdd(NodeAddEvent event) {
        if (!configManager.isAutoUpdateEnabled()) {
            return;
        }

        if (event.getTarget() instanceof User user) {
            handleNodeChange(user.getUniqueId(), "Node added");
        }

        if (event.getTarget() instanceof Group group) {
            handleGroupChange(group.getName(), "Node added to group", true);
        }
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        if (!configManager.isAutoUpdateEnabled()) {
            return;
        }

        if (event.getTarget() instanceof User user) {
            handleNodeChange(user.getUniqueId(), "Node removed");
        }

        if (event.getTarget() instanceof Group group) {
            handleGroupChange(group.getName(), "Node removed from group", true);
        }
    }

    private void onGroupDataRecalculate(GroupDataRecalculateEvent event) {
        if (!configManager.isAutoUpdateEnabled()) {
            return;
        }

        handleGroupChange(event.getGroup().getName(), "Group data recalculated", true);
    }

    private void handleNodeChange(UUID uuid, String reason) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                return;
            }

            teamManager.updatePlayerTeam(player);
            chatManager.updatePlayerListName(player);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Auto-updated " + player.getName() + " (" + reason + ")");
            }
        }, 5L);
    }

    private void handleGroupChange(String groupName, String reason, boolean saveToConfig) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (saveToConfig) {
                GroupManager.GroupData groupData = groupManager.getGroup(groupName);

                if (groupData != null) {
                    groupManager.saveToConfig(groupName);

                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Auto-saved group '" + groupName + "' to config");
                    }
                }
            }

            int updatedCount = 0;

            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);

                if (user == null) {
                    continue;
                }

                String primaryGroup = user.getPrimaryGroup();

                if (!primaryGroup.equalsIgnoreCase(groupName)) {
                    continue;
                }

                teamManager.updatePlayerTeam(player);
                chatManager.updatePlayerListName(player);
                updatedCount++;
            }

            if (configManager.isDebugEnabled() && updatedCount > 0) {
                plugin.getLogger().info("Auto-updated " + updatedCount + " players in group '" + groupName + "' (" + reason + ")");
            }
        }, 10L);
    }
}
