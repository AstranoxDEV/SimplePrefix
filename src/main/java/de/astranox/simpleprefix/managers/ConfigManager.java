package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.update.UpdateChannel;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final SimplePrefix plugin;
    private FileConfiguration config;
    private File configFile;
    private long lastModified;

    public ConfigManager(SimplePrefix plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        configFile = new File(plugin.getDataFolder(), "config.yml");
        lastModified = configFile.lastModified();
        if(!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        lastModified = configFile.lastModified();
        plugin.getLogger().info("Config reloaded!");
    }

    public boolean checkAndReload() {
        long current = configFile.lastModified();

        if (current <= lastModified) {
            return false;
        }

        reloadConfig();
        return true;
    }

    public boolean isAutoReloadEnabled() {
        return config.getBoolean("settings.auto-reload.enabled", true);
    }

    public int getReloadInterval() {
        return config.getInt("settings.auto-reload.interval", 30);
    }

    public long getJoinDelay() {
        return config.getLong("settings.join-delay", 20L);
    }

    public String getTeamPrefix() {
        return config.getString("settings.team-prefix", "lp_");
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    public boolean isAutoUpdateEnabled() {
        return config.getBoolean("settings.auto-update.enabled", true);
    }

    public boolean isChatFormatEnabled() {
        return config.getBoolean("formats.chat.enabled", true);
    }

    public String getChatFormat() {
        return config.getString("formats.chat.format", "{prefix}{player}{suffix}: {message}");
    }

    public void setChatFormat(String format) {
        config.set("formats.chat.format", format);
        saveConfig();
    }

    public void setChatEnabled(boolean enabled) {
        config.set("formats.chat.enabled", enabled);
        saveConfig();
    }

    public boolean isTabFormatEnabled() {
        return config.getBoolean("formats.tab.enabled", true);
    }

    public String getTabFormat() {
        return config.getString("formats.tab.format", "{prefix}{player}{suffix}");
    }

    public void setTabFormat(String format) {
        config.set("formats.tab.format", format);
        saveConfig();
    }

    public void setTabEnabled(boolean enabled) {
        config.set("formats.tab.enabled", enabled);
        saveConfig();
    }

    public boolean isUpdateCheckEnabled() {
        return config.getBoolean("settings.update-check.enabled", true);
    }

    public int getUpdateCheckInterval() {
        return config.getInt("settings.update-check.interval", 86400);
    }

    public UpdateChannel getUpdateChannel() {
        String channel = config.getString("settings.update-check.channel", "stable");
        return UpdateChannel.fromString(channel);
    }

    public void setUpdateChannel(UpdateChannel channel) {
        config.set("settings.update-check.channel", channel.name().toLowerCase());
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
            lastModified = configFile.lastModified();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String path) {
        return config.getString("messages." + path, "").replace('&', '§');
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
