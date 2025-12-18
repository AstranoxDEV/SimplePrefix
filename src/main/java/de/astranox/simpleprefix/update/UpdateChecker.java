package de.astranox.simpleprefix.update;

import de.astranox.simpleprefix.SimplePrefix;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {

    private static final String UPDATE_URL = "https://api.astranox.de/simpleprefix/latest";

    private final SimplePrefix plugin;
    private VersionComparator versionComparator;
    private final UpdateDownloader downloader;
    private final UpdateNotifier notifier;
    private final UpdateInstaller installer;

    private VersionInfo latestVersionInfo;
    private boolean updateAvailable = false;

    public UpdateChecker(SimplePrefix plugin) {
        this.plugin = plugin;
        UpdateChannel channel = plugin.getConfigManager().getUpdateChannel();
        this.versionComparator = new VersionComparator(channel);
        this.downloader = new UpdateDownloader(plugin);
        this.notifier = new UpdateNotifier(plugin);
        this.installer = new UpdateInstaller(plugin, downloader);
    }

    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {

                UpdateChannel channel = plugin.getConfigManager().getUpdateChannel();
                String url = UPDATE_URL + "?channel=" + channel.name().toLowerCase();

                plugin.getLogger().info("Checking for updates on channel: " + channel.getDisplayName());
                plugin.getLogger().info("Request URL: " + url);

                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "SimplePrefix/" + plugin.getDescription().getVersion());
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                int status = con.getResponseCode();
                if (status != 200) {
                    plugin.getLogger().warning("Update check failed: HTTP " + status);
                    return;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(response.toString());

                String version = (String) json.get("version");
                String downloadUrl = (String) json.get("download_url");
                String updaterUrl = (String) json.get("updater_url");

                if (version == null || version.isEmpty()) {
                    plugin.getLogger().warning("Invalid update response: version missing");
                    return;
                }

                latestVersionInfo = new VersionInfo(version, downloadUrl, updaterUrl);

                String currentVersion = plugin.getDescription().getVersion();

                this.versionComparator = new VersionComparator(channel);
                updateAvailable = versionComparator.shouldUpdate(currentVersion, version);

                if (updateAvailable) {
                    plugin.getLogger().info("Update channel: " + channel.getDisplayName());
                    notifier.notifyConsole(currentVersion, version, downloadUrl);
                } else {
                    plugin.getLogger().info("No update available for channel: " + channel.getDisplayName());
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates: " + e.getMessage());
            }
        });
    }

    public void setUpdateChannel(UpdateChannel channel) {
        plugin.getConfigManager().setUpdateChannel(channel);
        this.versionComparator = new VersionComparator(channel);
        plugin.getLogger().info("Update channel changed to: " + channel.getDisplayName());
    }

    public void notifyPlayer(Player player) {
        if (!updateAvailable || latestVersionInfo == null) return;
        notifier.notifyPlayer(player, plugin.getDescription().getVersion(), latestVersionInfo.getVersion());
    }

    public CompletableFuture<UpdateResult> installUpdate(boolean hotReload) {
        if (!updateAvailable || latestVersionInfo == null) {
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "No update available")
            );
        }

        if (latestVersionInfo.getDownloadUrl() == null || latestVersionInfo.getDownloadUrl().isEmpty()) {
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "Download URL missing")
            );
        }

        if (hotReload) {
            if (latestVersionInfo.getUpdaterUrl() == null || latestVersionInfo.getUpdaterUrl().isEmpty()) {
                return CompletableFuture.completedFuture(
                        new UpdateResult(false, "Updater plugin URL missing")
                );
            }
            return installer.installHotReload(
                    latestVersionInfo.getDownloadUrl(),
                    latestVersionInfo.getUpdaterUrl()
            );
        } else {
            return installer.installStandard(
                    latestVersionInfo.getDownloadUrl(),
                    latestVersionInfo.getVersion()
            );
        }
    }

    public void resetNotifications() {
        notifier.reset();
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public VersionInfo getLatestVersionInfo() {
        return latestVersionInfo;
    }

    public boolean isInstalling() {
        return installer.isInstalling();
    }

    public UpdateChannel getCurrentChannel() {
        return plugin.getConfigManager().getUpdateChannel();
    }
}