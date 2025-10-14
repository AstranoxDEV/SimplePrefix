package de.syntaxjasom.simplePrefix.managers;

import de.syntaxjasom.simplePrefix.SimplePrefix;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class MigrationManager {

    private final SimplePrefix plugin;
    private final GroupManager groupManager;
    private final ConfigManager configManager;

    public MigrationManager(SimplePrefix plugin, GroupManager groupManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
        this.configManager = configManager;
    }

    public MigrationResult migrateLuckPrefix() {
        File luckPrefixFolder = new File(plugin.getDataFolder().getParentFile(), "LuckPrefix");
        File luckPrefixGroupsFile = new File(luckPrefixFolder, "groups.yml");

        if (!luckPrefixGroupsFile.exists()) {
            return new MigrationResult(false, 0, "LuckPrefix groups.yml not found at: " + luckPrefixGroupsFile.getAbsolutePath());
        }

        try {
            YamlConfiguration luckPrefixConfig = YamlConfiguration.loadConfiguration(luckPrefixGroupsFile);
            Set<String> groups = luckPrefixConfig.getKeys(false);

            if (groups.isEmpty()) {
                return new MigrationResult(false, 0, "No groups found in LuckPrefix config");
            }

            String migratedChatFormat = null;
            String migratedTabFormat = null;

            int migratedCount = 0;
            int skippedCount = 0;
            Map<String, String> details = new LinkedHashMap<>();

            for (String groupName : groups) {
                ConfigurationSection groupSection = luckPrefixConfig.getConfigurationSection(groupName);

                if (groupSection == null) {
                    skippedCount++;
                    details.put(groupName, "Empty/null group");
                    plugin.getLogger().info("Skipping group '" + groupName + "': null section");
                    continue;
                }

                String prefix = groupSection.getString("Prefix", "");
                String suffix = groupSection.getString("Suffix", "");
                int sortId = groupSection.getInt("SortID", 999);
                String nameColor = groupSection.getString("NameColor", "");
                String chatFormat = groupSection.getString("Chatformat", "");
                String tabFormat = groupSection.getString("Tabformat", "");

                if (migratedChatFormat == null && chatFormat != null && !chatFormat.trim().isEmpty()) {
                    migratedChatFormat = convertFormat(chatFormat);
                    plugin.getLogger().info("Migrating chat format from group '" + groupName + "': " + migratedChatFormat);
                }

                if (migratedTabFormat == null && tabFormat != null && !tabFormat.trim().isEmpty()) {
                    migratedTabFormat = convertFormat(tabFormat);
                    plugin.getLogger().info("Migrating tab format from group '" + groupName + "': " + migratedTabFormat);
                }

                if ((prefix == null || prefix.trim().isEmpty()) && (suffix == null || suffix.trim().isEmpty())) {
                    skippedCount++;
                    details.put(groupName, "No prefix/suffix (empty)");
                    plugin.getLogger().info("Skipping group '" + groupName + "': no prefix or suffix");
                    continue;
                }

                int priority = sortId;

                plugin.getLogger().info("Migrating '" + groupName + "': prefix='" + prefix + "', suffix='" + suffix + "', sortId=" + sortId + " → priority=" + priority + ", nameColor=" + nameColor);

                groupManager.setGroup(groupName, prefix, suffix, priority, nameColor);
                details.put(groupName, "✓ Migrated (Priority: " + priority + ")");
                migratedCount++;
            }

            if (migratedChatFormat != null) {
                configManager.setChatFormat(migratedChatFormat);
                details.put("[Chat Format]", "✓ Migrated: " + migratedChatFormat);
                plugin.getLogger().info("Chat format migrated successfully!");
            }

            if (migratedTabFormat != null) {
                configManager.setTabFormat(migratedTabFormat);
                details.put("[Tab Format]", "✓ Migrated: " + migratedTabFormat);
                plugin.getLogger().info("Tab format migrated successfully!");
            }

            String message = "Migrated " + migratedCount + " groups";
            if (skippedCount > 0) {
                message += " (skipped " + skippedCount + " empty groups)";
            }
            if (migratedChatFormat != null || migratedTabFormat != null) {
                message += " and formats";
            }

            return new MigrationResult(true, migratedCount, message, details);

        } catch (Exception e) {
            plugin.getLogger().severe("Error during migration: " + e.getMessage());
            e.printStackTrace();
            return new MigrationResult(false, 0, "Migration failed: " + e.getMessage());
        }
    }

    private String convertFormat(String format) {
        if (format == null || format.isEmpty()) {
            return "";
        }

        format = format.replace("<prefix>", "{prefix}");
        format = format.replace("<suffix>", "{suffix}");
        format = format.replace("<player>", "{player}");
        format = format.replace("<displayname>", "{displayname}");
        format = format.replace("<message>", "{message}");

        return format;
    }

    public MigrationResult cleanupEmptyGroups() {
        try {
            Map<String, GroupManager.GroupData> allGroups = groupManager.getAllGroups();
            int removedCount = 0;
            List<String> removedGroups = new ArrayList<>();

            for (Map.Entry<String, GroupManager.GroupData> entry : allGroups.entrySet()) {
                String groupName = entry.getKey();
                GroupManager.GroupData data = entry.getValue();

                boolean prefixEmpty = data.prefix == null || data.prefix.trim().isEmpty();
                boolean suffixEmpty = data.suffix == null || data.suffix.trim().isEmpty();

                if (prefixEmpty && suffixEmpty) {
                    groupManager.deleteGroup(groupName);
                    removedGroups.add(groupName);
                    removedCount++;
                    plugin.getLogger().info("Removed empty group: " + groupName);
                }
            }

            if (removedCount == 0) {
                return new MigrationResult(true, 0, "No empty groups found to cleanup");
            }

            Map<String, String> details = new LinkedHashMap<>();
            for (String group : removedGroups) {
                details.put(group, "Removed");
            }

            return new MigrationResult(true, removedCount, "Cleaned up " + removedCount + " empty groups", details);

        } catch (Exception e) {
            plugin.getLogger().severe("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
            return new MigrationResult(false, 0, "Cleanup failed: " + e.getMessage());
        }
    }

    public static class MigrationResult {
        public final boolean success;
        public final int migratedCount;
        public final String message;
        public final Map<String, String> details;

        public MigrationResult(boolean success, int migratedCount, String message) {
            this(success, migratedCount, message, new HashMap<>());
        }

        public MigrationResult(boolean success, int migratedCount, String message, Map<String, String> details) {
            this.success = success;
            this.migratedCount = migratedCount;
            this.message = message;
            this.details = details;
        }
    }
}
