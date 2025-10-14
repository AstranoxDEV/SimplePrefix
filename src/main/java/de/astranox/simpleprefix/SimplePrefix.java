package de.astranox.simpleprefix;

import de.astranox.simpleprefix.commands.PrefixCommand;
import de.astranox.simpleprefix.handlers.ConfigWatcher;
import de.astranox.simpleprefix.handlers.LuckPermsEventHandler;
import de.astranox.simpleprefix.handlers.PlayerJoinHandler;
import de.astranox.simpleprefix.managers.*;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Arrays;

public class SimplePrefix extends JavaPlugin implements Listener {

    private LuckPermsWrapper luckPermsWrapper;
    private Scoreboard scoreboard;
    private boolean useLuckPerms = false;

    private ConfigManager configManager;
    private GroupManager groupManager;
    private PermissionGroupResolver permissionGroupResolver;
    private TeamManager teamManager;
    private ChatManager chatManager;
    private MigrationManager migrationManager;
    private ConfigWatcher configWatcher;
    private LuckPermsEventHandler luckPermsEventHandler;

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
        teamManager = new TeamManager(this, luckPermsWrapper, scoreboard, configManager, groupManager, permissionGroupResolver, useLuckPerms);
        chatManager = new ChatManager(this, luckPermsWrapper, configManager, groupManager, permissionGroupResolver, useLuckPerms);
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
            luckPermsEventHandler = new LuckPermsEventHandler(this, luckPermsWrapper, teamManager, chatManager, configManager, groupManager);
            luckPermsEventHandler.register();
        }
    }

    private void registerCommands() {
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            PrefixCommand prefixCommand = new PrefixCommand(this, configManager, groupManager, teamManager, chatManager, migrationManager);
            commands.register(prefixCommand.buildCommand().build(), "SimplePrefix commands", Arrays.asList("simpleprefix", "sp"));
        });
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
                teamManager.updatePlayerTeam(player);
                chatManager.updatePlayerListName(player);
            }
        }, 20L);
    }

    private void cleanupTeams() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            teamManager.removePlayerTeam(player);
        }
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            teamManager.updatePlayerTeam(player);
            chatManager.updatePlayerListName(player);
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

    public ChatManager getChatManager() {
        return chatManager;
    }

    public MigrationManager getMigrationManager() {
        return migrationManager;
    }

    public ConfigWatcher getConfigWatcher() {
        return configWatcher;
    }
}
