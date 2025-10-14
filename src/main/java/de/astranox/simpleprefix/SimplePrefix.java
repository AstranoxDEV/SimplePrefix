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

    private LuckPerms luckPerms;
    private Scoreboard scoreboard;

    private ConfigManager configManager;
    private GroupManager groupManager;
    private TeamManager teamManager;
    private ChatManager chatManager;
    private MigrationManager migrationManager;
    private ConfigWatcher configWatcher;
    private LuckPermsEventHandler luckPermsEventHandler;

    @Override
    public void onEnable() {
        if (!initializeLuckPerms()) {
            return;
        }

        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        initializeManagers();
        loadConfigurations();
        registerEvents();
        registerCommands();
        startWatchers();
        initializePlayers();

        getLogger().info("SimplePrefix enabled successfully!");
    }

    @Override
    public void onDisable() {
        stopWatchers();
        cleanupTeams();

        getLogger().info("SimplePrefix disabled!");
    }

    private boolean initializeLuckPerms() {
        try {
            luckPerms = LuckPermsProvider.get();
            return true;
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms not found! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        groupManager = new GroupManager(this, luckPerms);
        teamManager = new TeamManager(this, luckPerms, scoreboard, configManager, groupManager);
        chatManager = new ChatManager(this, luckPerms, configManager, groupManager);
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

        luckPermsEventHandler = new LuckPermsEventHandler(this, luckPerms, teamManager, chatManager, configManager, groupManager);
        luckPermsEventHandler.register();
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

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
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
