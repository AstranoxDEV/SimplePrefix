package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.util.ComponentParser;
import de.astranox.simpleprefix.util.VersionUtil;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamManager {
    private final SimplePrefix plugin;
    private final LuckPermsWrapper luckPermsWrapper;
    private final Scoreboard scoreboard;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final PermissionGroupResolver permissionGroupResolver;
    private final ComponentParser componentParser;
    private final boolean useLuckPerms;

    private final int prefixLimit;
    private final int suffixLimit;

    public TeamManager(SimplePrefix plugin, LuckPermsWrapper luckPermsWrapper, Scoreboard scoreboard,
                       ConfigManager configManager, GroupManager groupManager,
                       PermissionGroupResolver permissionGroupResolver, boolean useLuckPerms) {
        this.plugin = plugin;
        this.luckPermsWrapper = luckPermsWrapper;
        this.scoreboard = scoreboard;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.permissionGroupResolver = permissionGroupResolver;
        this.componentParser = new ComponentParser(plugin);
        this.useLuckPerms = useLuckPerms;

        this.prefixLimit = VersionUtil.getPrefixLimit();
        this.suffixLimit = VersionUtil.getSuffixLimit();

        plugin.getLogger().info("TeamManager limits: prefix=" + prefixLimit + ", suffix=" + suffixLimit + " (MC " + VersionUtil.getVersionString() + ")");
    }

    public void updatePlayerTeam(Player player) {
        if (player == null || !player.isOnline()) return;

        String primaryGroup = resolvePlayerGroup(player);
        GroupManager.GroupData groupData = obtainGroupData(primaryGroup);
        if (groupData == null) return;

        String teamName = generateTeamName(player, primaryGroup, groupData);
        removeOldTeam(player);
        Team team = getOrCreateTeam(teamName);
        applyTeamFormat(team, player, groupData);
        setupTeamOptions(team);
        team.addEntry(player.getName());
    }

    private String resolvePlayerGroup(Player player) {
        if (useLuckPerms && luckPermsWrapper != null) return luckPermsWrapper.getPrimaryGroup(player);
        return permissionGroupResolver.resolveGroup(player);
    }

    private GroupManager.GroupData obtainGroupData(String group) {
        GroupManager.GroupData d = groupManager.getGroup(group);
        if (d != null) return d;
        return groupManager.getGroup("default");
    }

    private void removeOldTeam(Player player) {
        Team old = scoreboard.getPlayerTeam(player);
        if (old == null) return;
        old.removeEntry(player.getName());
        if (old.getSize() == 0) old.unregister();
    }

    private Team getOrCreateTeam(String name) {
        Team t = scoreboard.getTeam(name);
        if (t != null) return t;
        return scoreboard.registerNewTeam(name);
    }

    private void applyTeamFormat(Team team, Player player, GroupManager.GroupData data) {
        if (!configManager.isTabFormatEnabled()) {
            applySimplePrefixSuffix(team, data);
            return;
        }
        applyCustomTabFormat(team, player, data);
    }

    private void applySimplePrefixSuffix(Team team, GroupManager.GroupData data) {
        String parsedPrefix = componentParser.parse(data.prefix != null ? data.prefix : "");
        String parsedSuffix = componentParser.parse(data.suffix != null ? data.suffix : "");

        team.setPrefix(ensureTrailingSpace(parsedPrefix, prefixLimit));
        team.setSuffix(trimToPacketLimit(parsedSuffix, suffixLimit));
    }

    private void applyCustomTabFormat(Team team, Player player, GroupManager.GroupData data) {
        String parsedPrefix = componentParser.parse(data.prefix != null ? data.prefix : "");
        String parsedSuffix = componentParser.parse(data.suffix != null ? data.suffix : "");
        String parsedNameColor = componentParser.parse(data.nameColor != null && !data.nameColor.isEmpty() ? "<" + data.nameColor + ">" : "");

        String format = configManager.getTabFormat()
                .replace("{prefix}", parsedPrefix)
                .replace("{suffix}", parsedSuffix)
                .replace("{player}", "")
                .replace("{displayname}", "");

        String finalPrefix = ensureTrailingSpace(format + parsedNameColor, prefixLimit);
        team.setPrefix(finalPrefix);
        team.setSuffix("");
    }

    private String trimToPacketLimit(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        int count = 0;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                if (count + 2 > max) break;
                out.append(c).append(s.charAt(++i));
                count += 2;
                continue;
            }
            if (count + 1 > max) break;
            out.append(c);
            count++;
        }
        return out.toString();
    }

    private String ensureTrailingSpace(String text, int limit) {
        if (text == null) return " ";
        text = trimToPacketLimit(text, limit);
        if (text.endsWith(" ")) return text;

        int len = packetLength(text);
        if (len < limit) return text;

        return trimToPacketLimit(text, limit - 1);
    }

    private int packetLength(String s) {
        if (s == null || s.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                count += 2;
                i++;
                continue;
            }
            count++;
        }
        return count;
    }

    private void setupTeamOptions(Team team) {
        if (team == null) return;

        if (VersionUtil.isLegacyVersion()) {
            try {
                team.setCanSeeFriendlyInvisibles(true);
                team.setAllowFriendlyFire(false);
            } catch (Throwable ignored) {}
            return;
        }

        try {
            Class<?> optionClass = Class.forName("org.bukkit.scoreboard.Team$Option");
            Class<?> statusClass = Class.forName("org.bukkit.scoreboard.Team$OptionStatus");

            Object nameTag = Enum.valueOf((Class<Enum>) optionClass, "NAME_TAG_VISIBILITY");
            Object collision = Enum.valueOf((Class<Enum>) optionClass, "COLLISION_RULE");
            Object always = Enum.valueOf((Class<Enum>) statusClass, "ALWAYS");

            team.getClass().getMethod("setOption", optionClass, statusClass).invoke(team, nameTag, always);
            team.getClass().getMethod("setOption", optionClass, statusClass).invoke(team, collision, always);
        } catch (Throwable t) {
            try {
                team.setCanSeeFriendlyInvisibles(true);
            } catch (Throwable ignored) {}
        }
    }

    public void removePlayerTeam(Player player) {
        if (player == null) return;
        Team t = scoreboard.getPlayerTeam(player);
        if (t == null) return;
        t.removeEntry(player.getName());
        if (t.getSize() == 0) t.unregister();
    }

    private String generateTeamName(Player player, String group, GroupManager.GroupData data) {
        int pr = data != null ? data.priority : 999;
        String sort = String.format("%03d", Math.max(0, Math.min(999, pr)));
        String tail = player.getName().toLowerCase().substring(0, Math.min(player.getName().length(), 10));
        return sort + "_" + tail;
    }
}