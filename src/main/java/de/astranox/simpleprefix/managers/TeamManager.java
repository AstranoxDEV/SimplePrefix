package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.util.ComponentParser;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamManager {

    private final SimplePrefix plugin;
    private final LuckPerms luckPerms;
    private final Scoreboard scoreboard;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final ComponentParser componentParser;

    public TeamManager(SimplePrefix plugin, LuckPerms luckPerms, Scoreboard scoreboard,
                       ConfigManager configManager, GroupManager groupManager) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.scoreboard = scoreboard;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.componentParser = new ComponentParser(plugin);
    }

    public void updatePlayerTeam(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            if (user == null) {
                plugin.getLogger().warning("Could not get LuckPerms user for " + player.getName());
                return;
            }

            String primaryGroup = user.getPrimaryGroup();

            GroupManager.GroupData groupData = groupManager.getGroup(primaryGroup);

            if (groupData == null) {
                plugin.getLogger().warning("Could not get group data for " + primaryGroup);
                return;
            }

            String prefix = groupData.prefix;
            String suffix = groupData.suffix;

            String teamName = generateTeamName(player, primaryGroup, groupData);

            removeOldTeam(player);

            Team team = getOrCreateTeam(teamName);

            updateTeamFormat(team, player, prefix, suffix, groupData);

            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);

            team.addEntry(player.getName());

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Updated team for " + player.getName() + " (Group: " + primaryGroup + ", Prefix: " + prefix + ")");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error updating team for " + player.getName());
            e.printStackTrace();
        }
    }

    private void removeOldTeam(Player player) {
        Team oldTeam = scoreboard.getPlayerTeam(player);

        if (oldTeam == null) {
            return;
        }

        oldTeam.removeEntry(player.getName());

        if (oldTeam.getSize() == 0) {
            oldTeam.unregister();
        }
    }

    private Team getOrCreateTeam(String teamName) {
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        return team;
    }

    private void updateTeamFormat(Team team, Player player, String prefix, String suffix,
                                  GroupManager.GroupData groupData) {
        if (!configManager.isTabFormatEnabled()) {
            updateLegacyFormat(team, prefix, suffix);
            return;
        }

        updateModernFormat(team, player, prefix, suffix, groupData);
    }

    private void updateLegacyFormat(Team team, String prefix, String suffix) {
        if (prefix != null && !prefix.isEmpty()) {
            team.prefix(componentParser.parse(prefix));
        }

        if (suffix != null && !suffix.isEmpty()) {
            team.suffix(componentParser.parse(suffix));
        }
    }

    private void updateModernFormat(Team team, Player player, String prefix, String suffix,
                                    GroupManager.GroupData groupData) {
        String format = configManager.getTabFormat();

        String nameColor = getNameColor(groupData);

        format = format.replace("{prefix}", prefix != null ? prefix : "");
        format = format.replace("{suffix}", suffix != null ? suffix : "");
        format = format.replace("{player}", "");
        format = format.replace("{displayname}", "");

        String fullPrefix = format + nameColor;

        if (!fullPrefix.isEmpty()) {
            team.prefix(componentParser.parse(fullPrefix));
        }
    }

    private String getNameColor(GroupManager.GroupData groupData) {
        if (groupData == null || groupData.nameColor == null || groupData.nameColor.isEmpty()) {
            return "";
        }

        return "<" + groupData.nameColor + ">";
    }

    public void removePlayerTeam(Player player) {
        if (player == null) {
            return;
        }

        Team team = scoreboard.getPlayerTeam(player);

        if (team == null) {
            return;
        }

        team.removeEntry(player.getName());

        if (team.getSize() == 0) {
            team.unregister();
        }
    }

    private String generateTeamName(Player player, String primaryGroup, GroupManager.GroupData groupData) {
        int priority = 999;
        if (groupData != null) {
            priority = groupData.priority;
        }

        String sortPrefix = String.format("%03d", Math.max(0, Math.min(999, priority)));
        String playerSuffix = player.getName().toLowerCase().substring(0, Math.min(player.getName().length(), 10));

        return sortPrefix + "_" + playerSuffix;
    }
}
