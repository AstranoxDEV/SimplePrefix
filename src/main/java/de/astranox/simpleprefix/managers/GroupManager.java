package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GroupManager {

    private final SimplePrefix plugin;
    private final LuckPermsWrapper luckPermsWrapper;
    private final boolean useLuckPerms;
    private File groupsFile;
    private FileConfiguration groupsConfig;
    private long lastModified;

    public GroupManager(SimplePrefix plugin, LuckPermsWrapper luckPermsWrapper, boolean useLuckPerms) {
        this.plugin = plugin;
        this.luckPermsWrapper = luckPermsWrapper;
        this.useLuckPerms = useLuckPerms;
    }

    public void loadGroups() {
        groupsFile = new File(plugin.getDataFolder(), "groups.yml");

        if (!groupsFile.exists()) {
            plugin.saveResource("groups.yml", false);
        }

        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
        lastModified = groupsFile.lastModified();

        if (useLuckPerms) {
            applyConfigOverridesToLuckPerms();
        }
    }

    public void reloadGroups() {
        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
        lastModified = groupsFile.lastModified();

        if (useLuckPerms) {
            applyConfigOverridesToLuckPerms();
        }

        plugin.getLogger().info("Groups config reloaded" + (useLuckPerms ? " and synced to LuckPerms!" : "!"));
    }

    public boolean checkAndReload() {
        long current = groupsFile.lastModified();

        if (current <= lastModified) {
            return false;
        }

        reloadGroups();
        return true;
    }

    public boolean createGroup(String groupName, String prefix, String suffix, int priority, String nameColor) {
        if (groupExists(groupName)) {
            plugin.getLogger().warning("Group '" + groupName + "' already exists!");
            return false;
        }

        if (useLuckPerms && luckPermsWrapper != null) {
            boolean created = luckPermsWrapper.createGroup(groupName);

            if (!created) {
                plugin.getLogger().severe("Failed to create group '" + groupName + "' in LuckPerms!");
                return false;
            }

            luckPermsWrapper.setGroupPrefix(groupName, prefix, priority);
            luckPermsWrapper.setGroupSuffix(groupName, suffix, priority);

            plugin.getLogger().info("Created group '" + groupName + "' in LuckPerms!");
        }

        saveToConfig(groupName, prefix, suffix, priority, nameColor);

        plugin.getLogger().info("Created group '" + groupName + "' in SimplePrefix!");

        return true;
    }

    public boolean groupExists(String groupName) {
        if (useLuckPerms && luckPermsWrapper != null) {
            return luckPermsWrapper.groupExists(groupName);
        }

        return groupsConfig.getConfigurationSection("groups." + groupName) != null;
    }

    public boolean deleteGroupCompletely(String groupName) {
        if (groupName.equalsIgnoreCase("default")) {
            plugin.getLogger().warning("Cannot delete default group!");
            return false;
        }

        if (!groupExists(groupName)) {
            plugin.getLogger().warning("Group '" + groupName + "' does not exist!");
            return false;
        }

        if (useLuckPerms && luckPermsWrapper != null) {
            boolean deleted = luckPermsWrapper.deleteGroup(groupName);

            if (deleted) {
                plugin.getLogger().info("Deleted group '" + groupName + "' from LuckPerms!");
            }
        }

        groupsConfig.set("groups." + groupName, null);

        try {
            groupsConfig.save(groupsFile);
            lastModified = groupsFile.lastModified();
            plugin.getLogger().info("Deleted group '" + groupName + "' from SimplePrefix!");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save groups.yml!");
            e.printStackTrace();
            return false;
        }
    }

    public void applyConfigOverridesToLuckPerms() {
        if (!useLuckPerms || luckPermsWrapper == null) {
            return;
        }

        ConfigurationSection groups = groupsConfig.getConfigurationSection("groups");

        if (groups == null) {
            plugin.getLogger().warning("No groups section found in groups.yml!");
            return;
        }

        int count = 0;
        for (String groupName : groups.getKeys(false)) {
            String prefix = groups.getString(groupName + ".prefix");
            String suffix = groups.getString(groupName + ".suffix");
            int priority = groups.getInt(groupName + ".priority", 0);

            if (!luckPermsWrapper.groupExists(groupName)) {
                plugin.getLogger().warning("Group '" + groupName + "' does not exist in LuckPerms!");
                continue;
            }

            boolean prefixSuccess = luckPermsWrapper.setGroupPrefix(groupName, prefix, priority);
            boolean suffixSuccess = luckPermsWrapper.setGroupSuffix(groupName, suffix, priority);

            if (prefixSuccess || suffixSuccess) {
                count++;
            }
        }

        plugin.getLogger().info("Applied " + count + " group overrides to LuckPerms!");
    }

    public Map<String, GroupData> getAllGroups() {
        Map<String, GroupData> result = new HashMap<>();

        ConfigurationSection groups = groupsConfig.getConfigurationSection("groups");

        if (groups == null) {
            return result;
        }

        for (String groupName : groups.getKeys(false)) {
            String prefix = groups.getString(groupName + ".prefix", "");
            String suffix = groups.getString(groupName + ".suffix", "");
            int priority = groups.getInt(groupName + ".priority", 999);
            String nameColor = groups.getString(groupName + ".nameColor");

            result.put(groupName, new GroupData(prefix, suffix, priority, nameColor));
        }

        return result;
    }

    public GroupData getGroup(String groupName) {
        ConfigurationSection groupSection = groupsConfig.getConfigurationSection("groups." + groupName);

        if (groupSection == null) {
            return null;
        }

        String prefix = groupSection.getString("prefix", "");
        String suffix = groupSection.getString("suffix", "");
        int priority = groupSection.getInt("priority", 999);
        String nameColor = groupSection.getString("nameColor");

        return new GroupData(prefix, suffix, priority, nameColor);
    }

    public void setGroup(String groupName, String prefix, String suffix, int priority) {
        setGroup(groupName, prefix, suffix, priority, null);
    }

    public void setGroup(String groupName, String prefix, String suffix, int priority, String nameColor) {
        if (useLuckPerms && luckPermsWrapper != null) {
            if (!luckPermsWrapper.groupExists(groupName)) {
                plugin.getLogger().warning("Group '" + groupName + "' does not exist in LuckPerms!");
            } else {
                luckPermsWrapper.setGroupPrefix(groupName, prefix, priority);
                luckPermsWrapper.setGroupSuffix(groupName, suffix, priority);
            }
        }

        saveToConfig(groupName, prefix, suffix, priority, nameColor);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.updateAllPlayers();
        }, 10L);
    }

    public void deleteGroup(String groupName) {
        if (useLuckPerms && luckPermsWrapper != null) {
            if (luckPermsWrapper.groupExists(groupName)) {
                luckPermsWrapper.setGroupPrefix(groupName, "", 0);
                luckPermsWrapper.setGroupSuffix(groupName, "", 0);
            }
        }

        groupsConfig.set("groups." + groupName, null);

        try {
            groupsConfig.save(groupsFile);
            lastModified = groupsFile.lastModified();
            plugin.getLogger().info("Removed group '" + groupName + "' from config!");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save groups.yml!");
            e.printStackTrace();
        }
    }

    public void saveToConfig(String groupName) {
        GroupData data = getGroup(groupName);

        if (data == null) {
            plugin.getLogger().warning("Group '" + groupName + "' not found!");
            return;
        }

        saveToConfig(groupName, data.prefix, data.suffix, data.priority, data.nameColor);
    }

    private void saveToConfig(String groupName, String prefix, String suffix, int priority, String nameColor) {
        if (plugin.getConfigWatcher() != null) {
            plugin.getConfigWatcher().setSaving(true);
        }

        groupsConfig.set("groups." + groupName + ".prefix", prefix != null && !prefix.isEmpty() ? prefix : "");
        groupsConfig.set("groups." + groupName + ".suffix", suffix != null && !suffix.isEmpty() ? suffix : "");
        groupsConfig.set("groups." + groupName + ".priority", priority);

        if (nameColor != null && !nameColor.isEmpty()) {
            groupsConfig.set("groups." + groupName + ".nameColor", nameColor);
        }

        try {
            groupsConfig.save(groupsFile);
            lastModified = groupsFile.lastModified();

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Saved group '" + groupName + "' to config!");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save groups.yml!");
            e.printStackTrace();
        } finally {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getConfigWatcher() != null) {
                    plugin.getConfigWatcher().setSaving(false);
                }
            }, 5L);
        }
    }

    public static class GroupData {
        public final String prefix;
        public final String suffix;
        public final int priority;
        public final String nameColor;

        public GroupData(String prefix, String suffix, int priority) {
            this(prefix, suffix, priority, null);
        }

        public GroupData(String prefix, String suffix, int priority, String nameColor) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.priority = priority;
            this.nameColor = nameColor;
        }
    }
}
