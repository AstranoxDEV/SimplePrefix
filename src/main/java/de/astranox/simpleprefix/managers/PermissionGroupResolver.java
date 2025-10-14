package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

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

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("=== Resolving group for " + player.getName() + " ===");
            plugin.getLogger().info("Available groups in config: " + allGroups.keySet());
            plugin.getLogger().info("Player's effective permissions:");
            for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
                if (perm.getPermission().startsWith("simpleprefix.group.")) {
                    plugin.getLogger().info("  - " + perm.getPermission() + " = " + perm.getValue());
                }
            }
        }

        List<GroupEntry> matchedGroups = new ArrayList<>();

        for (Map.Entry<String, GroupManager.GroupData> entry : allGroups.entrySet()) {
            String groupName = entry.getKey();
            GroupManager.GroupData data = entry.getValue();

            String permission = "simpleprefix.group." + groupName.toLowerCase();

            if (player.hasPermission(permission)) {
                matchedGroups.add(new GroupEntry(groupName, data.priority));

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("  ✓ Matched group: " + groupName + " (priority: " + data.priority + ")");
                }
            }
        }

        if (matchedGroups.isEmpty()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("  ! No groups matched, using default");
            }
            return "default";
        }

        matchedGroups.sort(Comparator.comparingInt(e -> e.priority));

        String resolvedGroup = matchedGroups.get(0).groupName;

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("  → Resolved to: " + resolvedGroup + " (priority: " + matchedGroups.get(0).priority + ")");
            if (matchedGroups.size() > 1) {
                plugin.getLogger().info("  (Player has " + matchedGroups.size() + " groups, showing highest priority)");
            }
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
