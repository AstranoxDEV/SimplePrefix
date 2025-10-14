package de.astranox.simpleprefix.handlers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.managers.ConfigManager;
import de.astranox.simpleprefix.managers.GroupManager;
import de.astranox.simpleprefix.managers.TeamManager;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigWatcher {

    private final SimplePrefix plugin;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final TeamManager teamManager;
    private WatchService watchService;
    private Thread watchThread;
    private final Map<String, Long> lastModified = new HashMap<>();
    private static final long DEBOUNCE_TIME = 1000; // 1 Sekunde
    private final AtomicBoolean isSaving = new AtomicBoolean(false);

    public ConfigWatcher(SimplePrefix plugin, ConfigManager configManager,
                         GroupManager groupManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.teamManager = teamManager;
    }

    public void setSaving(boolean saving) {
        isSaving.set(saving);
    }

    public void start() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path configPath = plugin.getDataFolder().toPath();

            if (!Files.exists(configPath)) {
                plugin.getLogger().warning("Config directory does not exist!");
                return;
            }

            configPath.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE
            );

            watchThread = new Thread(this::watch, "SimplePrefix-ConfigWatcher");
            watchThread.setDaemon(true);
            watchThread.start();

            plugin.getLogger().info("Config watcher started successfully!");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start config watcher!");
            e.printStackTrace();
        }
    }

    public void stop() {
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
            plugin.getLogger().info("Config watcher stopped!");
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void watch() {
        plugin.getLogger().info("Config watcher thread started!");

        while (!Thread.interrupted()) {
            try {
                WatchKey key = watchService.poll(1, java.util.concurrent.TimeUnit.SECONDS);

                if (key == null) {
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        plugin.getLogger().warning("Config watcher overflow!");
                        continue;
                    }

                    Path changed = (Path) event.context();
                    String fileName = changed.toString();

                    if (!shouldProcess(fileName)) {
                        continue;
                    }

                    if (isSaving.get()) {
                        if (configManager.getConfig().getBoolean("settings.debug", false)) {
                            plugin.getLogger().info("Ignoring change in " + fileName + " (currently saving)");
                        }
                        continue;
                    }

                    plugin.getLogger().info("Detected external change in: " + fileName);

                    if (fileName.equals("config.yml")) {
                        handleConfigChange();
                    }

                    if (fileName.equals("groups.yml")) {
                        handleGroupsChange();
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    plugin.getLogger().warning("Config watcher key no longer valid!");
                    break;
                }

            } catch (InterruptedException e) {
                plugin.getLogger().info("Config watcher interrupted!");
                break;
            } catch (Exception e) {
                plugin.getLogger().warning("Error in config watcher: " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Config watcher thread stopped!");
    }

    private boolean shouldProcess(String fileName) {
        if (fileName.endsWith(".tmp") || fileName.endsWith(".swp") || fileName.startsWith(".")) {
            return false;
        }

        if (!fileName.equals("config.yml") && !fileName.equals("groups.yml")) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long last = lastModified.get(fileName);

        if (last != null && (now - last) < DEBOUNCE_TIME) {
            return false;
        }

        lastModified.put(fileName, now);
        return true;
    }

    private void handleConfigChange() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                plugin.getLogger().info("Reloading config.yml...");
                configManager.reloadConfig();
                plugin.updateAllPlayers();
                plugin.getLogger().info("Config reloaded and players updated!");
            } catch (Exception e) {
                plugin.getLogger().severe("Error reloading config: " + e.getMessage());
                e.printStackTrace();
            }
        }, 10L);
    }

    private void handleGroupsChange() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                plugin.getLogger().info("Reloading groups.yml...");
                groupManager.reloadGroups();
                plugin.updateAllPlayers();
                plugin.getLogger().info("Groups reloaded and players updated!");
            } catch (Exception e) {
                plugin.getLogger().severe("Error reloading groups: " + e.getMessage());
                e.printStackTrace();
            }
        }, 10L);
    }
}
