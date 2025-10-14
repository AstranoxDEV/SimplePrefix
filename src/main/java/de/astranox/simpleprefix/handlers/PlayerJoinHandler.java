package de.astranox.simpleprefix.handlers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.managers.ChatManager;
import de.astranox.simpleprefix.managers.ConfigManager;
import de.astranox.simpleprefix.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinHandler implements Listener {

    private final SimplePrefix plugin;
    private final TeamManager teamManager;
    private final ChatManager chatManager;
    private final ConfigManager configManager;

    public PlayerJoinHandler(SimplePrefix plugin, TeamManager teamManager,
                             ChatManager chatManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.chatManager = chatManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            teamManager.updatePlayerTeam(player);
            chatManager.updatePlayerListName(player);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Initialized " + player.getName() + " on join");
            }
        }, configManager.getJoinDelay());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        teamManager.removePlayerTeam(event.getPlayer());

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Cleaned up team for " + event.getPlayer().getName());
        }
    }
}
