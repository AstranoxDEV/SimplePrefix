package de.astranox.simpleprefix.update;

import de.astranox.simpleprefix.SimplePrefix;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateInstaller {

    private final SimplePrefix plugin;
    private final UpdateDownloader downloader;
    private boolean isInstalling = false;

    public UpdateInstaller(SimplePrefix plugin, UpdateDownloader downloader) {
        this.plugin = plugin;
        this.downloader = downloader;
    }

    public CompletableFuture<UpdateResult> installStandard(String downloadUrl, String version) {
        if (isInstalling) {
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "Installation already in progress")
            );
        }

        isInstalling = true;

        return CompletableFuture.supplyAsync(() -> {
            try {

                File pluginsDir = plugin.getDataFolder().getParentFile();

                File currentJar = findPluginJarInDirectory(pluginsDir);

                String newFileName = "SimplePrefix-" + version + ".jar";
                File newJar = new File(pluginsDir, newFileName);

                plugin.getLogger().info("Downloading update to: " + newJar.getAbsolutePath());

                downloader.downloadFile(downloadUrl, newJar);

                if (currentJar != null && currentJar.exists() && !currentJar.equals(newJar)) {
                    File backup = new File(pluginsDir, currentJar.getName() + ".old");
                    plugin.getLogger().info("Marking old version for deletion: " + currentJar.getName());

                    File deleteMarker = new File(pluginsDir, currentJar.getName() + ".DELETE_ON_RESTART");
                    if (!deleteMarker.exists()) {
                        deleteMarker.createNewFile();
                    }

                    plugin.getLogger().info("Old version will be removed on next restart");
                }

                plugin.getLogger().info("✓ Update downloaded successfully: " + newJar.getName());
                plugin.getLogger().info("§e§l⚠ RESTART THE SERVER TO APPLY THE UPDATE!");

                isInstalling = false;
                return new UpdateResult(true, "Update downloaded! Restart server to apply.");

            } catch (Exception e) {
                isInstalling = false;
                plugin.getLogger().log(Level.SEVERE, "Failed to install update", e);
                return new UpdateResult(false, "Installation failed: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<UpdateResult> installHotReload(String downloadUrl, String updaterUrl) {
        if (isInstalling) {
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "Installation already in progress")
            );
        }

        isInstalling = true;

        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("§c§lHot-Reload Mode activated");

                Plugin updater = Bukkit.getPluginManager().getPlugin("SimplePrefixUpdater");

                if (updater == null) {
                    plugin.getLogger().info("SimplePrefixUpdater not found, downloading...");

                    File pluginsDir = plugin.getDataFolder().getParentFile();
                    File updaterJar = new File(pluginsDir, "SimplePrefixUpdater.jar");

                    downloader.downloadFile(updaterUrl, updaterJar);

                    plugin.getLogger().info("Loading SimplePrefixUpdater...");

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            Plugin loaded = Bukkit.getPluginManager().loadPlugin(updaterJar);
                            if (loaded != null) {
                                Bukkit.getPluginManager().enablePlugin(loaded);
                                plugin.getLogger().info("SimplePrefixUpdater loaded successfully");

                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    triggerHotReload(downloadUrl);
                                }, 20L);
                            } else {
                                plugin.getLogger().severe("Failed to load SimplePrefixUpdater!");
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.SEVERE, "Failed to load SimplePrefixUpdater", e);
                        }
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> triggerHotReload(downloadUrl));
                }

                isInstalling = false;
                return new UpdateResult(true, "Hot-reload initiated");

            } catch (Exception e) {
                isInstalling = false;
                plugin.getLogger().log(Level.SEVERE, "Hot-reload failed", e);
                return new UpdateResult(false, "Hot-reload failed: " + e.getMessage());
            }
        });
    }

    private void triggerHotReload(String downloadUrl) {
        plugin.getLogger().info("Triggering hot-reload via SimplePrefixUpdater...");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "spupdater hotreload " + downloadUrl);
    }

    private File findPluginJarInDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles((dir, name) ->
                name.startsWith("SimplePrefix") &&
                        name.endsWith(".jar") &&
                        !name.endsWith(".old") &&
                        !name.contains(".DELETE_ON_RESTART")
        );

        if (files != null && files.length > 0) {
            return files[0];
        }

        return null;
    }

    public boolean isInstalling() {
        return isInstalling;
    }
}