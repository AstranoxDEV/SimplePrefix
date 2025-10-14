package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PermissionGroupResolver {

    private final SimplePrefix plugin;
    private final GroupManager groupManager;

    public PermissionGroupResolver(SimplePrefix plugin, GroupManager groupManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
    }

    public String resolveGroup(Player player) {
        Map<String, GroupManager.GroupData> allGroups = groupManager.getAllGroups();

        List<GroupEntry> matchedGroups = new ArrayList<>();

        for (Map.Entry<String, GroupManager.GroupData> entry : allGroups.entrySet()) {
            String groupName = entry.getKey();
            GroupManager.GroupData data = entry.getValue();

            if (player.hasPermission("simpleprefix.group." + groupName)) {
                matchedGroups.add(new GroupEntry(groupName, data.priority));
            }
        }

        if (matchedGroups.isEmpty()) {
            return "default";
        }

        matchedGroups.sort(Comparator.comparingInt(e -> e.priority));

        String resolvedGroup = matchedGroups.get(0).groupName;

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Resolved group for " + player.getName() + ": " + resolvedGroup + " (priority: " + matchedGroups.get(0).priority + ")");
        }

        return resolvedGroup;
    }

    private static class GroupEntry {
        final String groupName;
        final int priority;

        GroupEntry(String groupName, int priority) {
            this.groupName = groupName;
            this.priority = priority;
        }
    }
}
