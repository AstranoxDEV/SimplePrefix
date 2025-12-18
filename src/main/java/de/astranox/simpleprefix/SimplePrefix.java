package de.astranox.simpleprefix;

import de.astranox.simpleprefix.commands.PrefixCommand;
import de.astranox.simpleprefix.handlers.ConfigWatcher;
import de.astranox.simpleprefix.handlers.LuckPermsEventHandler;
import de.astranox.simpleprefix.handlers.PlayerJoinHandler;
import de.astranox.simpleprefix.managers.*;
import de.astranox.simpleprefix.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.util.logging.Level;

public class SimplePrefix extends JavaPlugin implements Listener {
    private LuckPermsWrapper luckPermsWrapper;
    private Scoreboard scoreboard;
    private boolean useLuckPerms = false;
    private ConfigManager configManager;
    private GroupManager groupManager;
    private PermissionGroupResolver permissionGroupResolver;
    private TeamManager teamManager;
    private TabChatManager chatManager;
    private MigrationManager migrationManager;
    private ConfigWatcher configWatcher;
    private LuckPermsEventHandler luckPermsEventHandler;

    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        initializeLuckPerms();
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        initializeManagers();
        loadConfigurations();
        registerEvents();
        registerCommands();
        startWatchers();
        initializePlayers();

        cleanupOldVersions();

        updateChecker = new UpdateChecker(this);
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> updateChecker.checkForUpdates(), 40L);

        if (useLuckPerms) {
            getLogger().info("SimplePrefix enabled with LuckPerms integration!");
        } else {
            getLogger().info("SimplePrefix enabled in permission-based mode!");
            getLogger().info("Use permissions: simpleprefix.group.<groupname>");
        }
    }

    @Override
    public void onDisable() {
        stopWatchers();
        cleanupTeams();
        getLogger().info("SimplePrefix disabled!");
    }

    private void initializeLuckPerms() {
        try {
            Class.forName("net.luckperms.api.LuckPerms");
            luckPermsWrapper = new LuckPermsWrapper();
            useLuckPerms = true;
            getLogger().info("LuckPerms detected! Using LuckPerms for group management.");
        } catch (ClassNotFoundException e) {
            getLogger().info("LuckPerms not found! Using permission-based group system.");
            useLuckPerms = false;
        } catch (IllegalStateException e) {
            getLogger().warning("LuckPerms found but not loaded yet! Using permission-based group system.");
            useLuckPerms = false;
        } catch (Exception e) {
            getLogger().warning("Error initializing LuckPerms: " + e.getMessage());
            getLogger().info("Falling back to permission-based group system.");
            useLuckPerms = false;
        }
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        groupManager = new GroupManager(this, luckPermsWrapper, useLuckPerms);
        permissionGroupResolver = new PermissionGroupResolver(this, groupManager);
        teamManager = new TeamManager(this, luckPermsWrapper, configManager, groupManager, permissionGroupResolver);
        chatManager = new TabChatManager(this, luckPermsWrapper, configManager,
                groupManager, permissionGroupResolver);
        migrationManager = new MigrationManager(this, groupManager, configManager);
    }

    private void loadConfigurations() {
        configManager.loadConfigs();
        groupManager.loadGroups();
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(chatManager, this);
        PlayerJoinHandler joinHandler = new PlayerJoinHandler(this, teamManager, chatManager, configManager);
        getServer().getPluginManager().registerEvents(joinHandler, this);

        if (useLuckPerms && luckPermsWrapper != null) {
            luckPermsEventHandler = new LuckPermsEventHandler(this, luckPermsWrapper, teamManager,
                    chatManager, configManager, groupManager);
            luckPermsEventHandler.register();
        }
    }

    private void registerCommands() {
        PrefixCommand prefixCommand = new PrefixCommand(this, configManager, groupManager,
                teamManager, chatManager, migrationManager);
        getCommand("simpleprefix").setExecutor(prefixCommand);
        getCommand("simpleprefix").setTabCompleter(prefixCommand);

        getCommand("sp").setExecutor(prefixCommand);
        getCommand("sp").setTabCompleter(prefixCommand);
    }

    private void startWatchers() {
        configWatcher = new ConfigWatcher(this, configManager, groupManager, teamManager);
        configWatcher.start();
    }

    private void stopWatchers() {
        if (configWatcher != null) {
            configWatcher.stop();
        }
    }

    private void initializePlayers() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                teamManager.updatePlayer(player);
            }
        }, 20L);
    }

    private void cleanupTeams() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            teamManager.removePlayer(player);
        }
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            teamManager.updatePlayer(player);
        }
    }

    private void cleanupOldVersions() {
        File pluginsDir = getDataFolder().getParentFile();

        File[] filesToDelete = pluginsDir.listFiles((dir, name) ->
                name.startsWith("SimplePrefix") &&
                        (name.endsWith(".old") || name.contains(".DELETE_ON_RESTART"))
        );

        if (filesToDelete != null && filesToDelete.length > 0) {
            for (File file : filesToDelete) {
                try {
                    String originalName = file.getName()
                            .replace(".old", "")
                            .replace(".DELETE_ON_RESTART", "");

                    File original = new File(pluginsDir, originalName);

                    if (file.getName().contains(".DELETE_ON_RESTART")) {
                        file.delete();

                        if (original.exists()) {
                            boolean deleted = original.delete();
                            if (deleted) {
                                getLogger().info("Removed old version: " + original.getName());
                            } else {
                                getLogger().warning("Could not remove old version: " + original.getName());
                            }
                        }
                    }
                    else if (file.getName().endsWith(".old")) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            getLogger().info("Cleaned up backup: " + file.getName());
                        }
                    }
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Failed to cleanup: " + file.getName(), e);
                }
            }
        }
    }

    public boolean isUsingLuckPerms() {
        return useLuckPerms;
    }

    public LuckPermsWrapper getLuckPermsWrapper() {
        return luckPermsWrapper;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public PermissionGroupResolver getPermissionGroupResolver() {
        return permissionGroupResolver;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public TabChatManager getChatManager() {
        return chatManager;
    }

    public MigrationManager getMigrationManager() {
        return migrationManager;
    }

    public ConfigWatcher getConfigWatcher() {
        return configWatcher;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}