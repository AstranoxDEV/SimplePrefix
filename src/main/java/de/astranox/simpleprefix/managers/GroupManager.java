package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
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
    private final LuckPerms luckPerms;
    private File groupsFile;
    private FileConfiguration groupsConfig;
    private long lastModified;

    public GroupManager(SimplePrefix plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void loadGroups() {
        groupsFile = new File(plugin.getDataFolder(), "groups.yml");

        if (!groupsFile.exists()) {
            plugin.saveResource("groups.yml", false);
        }

        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
        lastModified = groupsFile.lastModified();

        applyConfigOverridesToLuckPerms();
    }

    public void reloadGroups() {
        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
        lastModified = groupsFile.lastModified();

        applyConfigOverridesToLuckPerms();

        plugin.getLogger().info("Groups config reloaded and synced to LuckPerms!");
    }

    public boolean checkAndReload() {
        long current = groupsFile.lastModified();

        if (current <= lastModified) {
            return false;
        }

        reloadGroups();
        return true;
    }

    public void applyConfigOverridesToLuckPerms() {
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

            boolean success = setGroupInLuckPerms(groupName, prefix, suffix, priority);
            if (success) {
                count++;
            }
        }

        plugin.getLogger().info("Applied " + count + " group overrides to LuckPerms!");
    }

    public Map<String, GroupData> getAllGroups() {
        Map<String, GroupData> result = new HashMap<>();

        for (Group group : luckPerms.getGroupManager().getLoadedGroups()) {
            String groupName = group.getName();

            String prefix = null;
            String suffix = null;
            int priority = 0;

            for (PrefixNode node : group.getNodes(NodeType.PREFIX)) {
                prefix = node.getMetaValue();
                priority = node.getPriority();
                break;
            }

            for (SuffixNode node : group.getNodes(NodeType.SUFFIX)) {
                suffix = node.getMetaValue();
                break;
            }

            String nameColor = null;
            ConfigurationSection groupSection = groupsConfig.getConfigurationSection("groups." + groupName);
            if (groupSection != null) {
                nameColor = groupSection.getString("nameColor");
            }

            result.put(groupName, new GroupData(prefix, suffix, priority, nameColor));
        }

        return result;
    }

    public GroupData getGroup(String groupName) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);

        if (group == null) {
            return null;
        }

        String prefix = null;
        String suffix = null;
        int priority = 0;
        String nameColor = null;

        for (PrefixNode node : group.getNodes(NodeType.PREFIX)) {
            prefix = node.getMetaValue();
            priority = node.getPriority();
            break;
        }

        for (SuffixNode node : group.getNodes(NodeType.SUFFIX)) {
            suffix = node.getMetaValue();
            break;
        }

        ConfigurationSection groupSection = groupsConfig.getConfigurationSection("groups." + groupName);
        if (groupSection != null) {
            nameColor = groupSection.getString("nameColor");
        }

        return new GroupData(prefix, suffix, priority, nameColor);
    }

    public void setGroup(String groupName, String prefix, String suffix, int priority) {
        setGroup(groupName, prefix, suffix, priority, null);
    }

    public void setGroup(String groupName, String prefix, String suffix, int priority, String nameColor) {
        setGroupInLuckPerms(groupName, prefix, suffix, priority);
        saveToConfig(groupName, prefix, suffix, priority, nameColor);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.updateAllPlayers();
        }, 10L);
    }

    private boolean setGroupInLuckPerms(String groupName, String prefix, String suffix, int priority) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);

        if (group == null) {
            plugin.getLogger().warning("Group '" + groupName + "' does not exist in LuckPerms!");
            return false;
        }

        group.data().clear(node -> node instanceof PrefixNode);
        group.data().clear(node -> node instanceof SuffixNode);

        if (prefix != null && !prefix.isEmpty()) {
            group.data().add(PrefixNode.builder(prefix, priority).build());
        }

        if (suffix != null && !suffix.isEmpty()) {
            group.data().add(SuffixNode.builder(suffix, priority).build());
        }

        luckPerms.getGroupManager().saveGroup(group);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Updated group '" + groupName + "' in LuckPerms: prefix='" + prefix + "', suffix='" + suffix + "', priority=" + priority);
        }

        return true;
    }

    public void deleteGroup(String groupName) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);

        if (group != null) {
            group.data().clear(node -> node instanceof PrefixNode);
            group.data().clear(node -> node instanceof SuffixNode);
            luckPerms.getGroupManager().saveGroup(group);
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
            // Entferne Save-Lock nach kurzer Zeit
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
