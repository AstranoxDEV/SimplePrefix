package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.util.ComponentParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TabChatManager implements Listener {

    private final SimplePrefix plugin;
    private final LuckPermsWrapper luckPermsWrapper;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final PermissionGroupResolver permissionGroupResolver;
    private final ComponentParser componentParser;
    private final boolean useLuckPerms;
    private final int prefixLimit;
    private final int suffixLimit;
    private final Scoreboard scoreboard;

    public TabChatManager(SimplePrefix plugin, LuckPermsWrapper luckPermsWrapper, ConfigManager configManager,
                          GroupManager groupManager, PermissionGroupResolver permissionGroupResolver,
                          boolean useLuckPerms) {
        this.plugin = plugin;
        this.luckPermsWrapper = luckPermsWrapper;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.permissionGroupResolver = permissionGroupResolver;
        this.componentParser = new ComponentParser(plugin);
        this.useLuckPerms = useLuckPerms;

        this.prefixLimit = 16;
        this.suffixLimit = 16;

        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!configManager.isChatFormatEnabled()) return;
        Player player = event.getPlayer();
        String primaryGroup = resolvePlayerGroup(player);
        GroupManager.GroupData groupData = obtainGroupData(primaryGroup);
        if (groupData == null) return;

        String format = configManager.getChatFormat();
        String nameColor = groupData.nameColor != null && !groupData.nameColor.isEmpty()
                ? "<" + groupData.nameColor + ">" : "";

        format = format.replace("{prefix}", groupData.prefix != null ? groupData.prefix : "");
        format = format.replace("{suffix}", groupData.suffix != null ? groupData.suffix : "");
        format = format.replace("{player}", nameColor + player.getName());
        format = format.replace("{displayname}", nameColor + player.getDisplayName());
        format = format.replace("{message}", event.getMessage());

        String parsed = componentParser.parse(format);
        event.setFormat(parsed.replace(event.getMessage(), "%2$s"));
    }

    public void updatePlayerListName(Player player) {
        if (!configManager.isTabFormatEnabled()) {

            removePlayerTeam(player);
            return;
        }

        String primaryGroup = resolvePlayerGroup(player);
        GroupManager.GroupData groupData = obtainGroupData(primaryGroup);
        if (groupData == null) return;

        String format = configManager.getTabFormat();
        String nameColor = groupData.nameColor != null && !groupData.nameColor.isEmpty()
                ? "<" + groupData.nameColor + ">" : "";

        String prefix = groupData.prefix != null ? groupData.prefix : "";
        String suffix = groupData.suffix != null ? groupData.suffix : "";

        if (format.contains("{player}")) {

            String[] parts = format.split("\\{player\\}", 2);
            prefix = parts.length > 0 ? parts[0] : "";
            suffix = parts.length > 1 ? parts[1] : "";

            prefix = prefix.replace("{prefix}", groupData.prefix != null ? groupData.prefix : "");
            prefix = prefix.replace("{suffix}", "");
            prefix = prefix + nameColor;

            suffix = suffix.replace("{suffix}", groupData.suffix != null ? groupData.suffix : "");
            suffix = suffix.replace("{prefix}", "");
        } else {

            prefix = componentParser.parse(prefix);
            suffix = componentParser.parse(suffix);
        }

        prefix = componentParser.parse(prefix);
        suffix = componentParser.parse(suffix);

        prefix = trimToPacketLimit(prefix, prefixLimit);
        suffix = trimToPacketLimit(suffix, suffixLimit);

        setPlayerTeam(player, prefix, suffix);
    }

    private void setPlayerTeam(Player player, String prefix, String suffix) {

        String teamName = "sp_" + player.getName().substring(0, Math.min(player.getName().length(), 10));

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        try {
            team.setPrefix(prefix);
            team.setSuffix(suffix);
        } catch (IllegalArgumentException e) {
            team.setPrefix(prefix.substring(0, Math.min(prefix.length(), prefixLimit)));
            team.setSuffix(suffix.substring(0, Math.min(suffix.length(), suffixLimit)));
        }

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }
    }

    private void removePlayerTeam(Player player) {
        String teamName = "sp_" + player.getName().substring(0, Math.min(player.getName().length(), 10));
        Team team = scoreboard.getTeam(teamName);

        if (team != null) {
            team.removeEntry(player.getName());

            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private String resolvePlayerGroup(Player player) {
        if (useLuckPerms && luckPermsWrapper != null) return luckPermsWrapper.getPrimaryGroup(player);
        return permissionGroupResolver.resolveGroup(player);
    }

    private GroupManager.GroupData obtainGroupData(String primaryGroup) {
        GroupManager.GroupData data = groupManager.getGroup(primaryGroup);
        if (data != null) return data;
        return groupManager.getGroup("default");
    }

    private String trimToPacketLimit(String text, int max) {
        if (text == null || text.isEmpty()) return "";
        int count = 0;
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                if (count + 2 > max) break;
                out.append(c).append(text.charAt(++i));
                count += 2;
                continue;
            }

            if (count + 1 > max) break;
            out.append(c);
            count++;
        }

        return out.toString();
    }

    public String parseFormatting(String text) {
        return componentParser.parse(text);
    }
}