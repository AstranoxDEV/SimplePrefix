package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.util.ComponentParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamManager {

    private static final int PREFIX_LIMIT = 256;
    private static final int SUFFIX_LIMIT = 256;
    private static final String TEAM_NS = "sp";

    private final SimplePrefix plugin;
    private final Scoreboard scoreboard;
    private final ConfigManager config;
    private final GroupManager groups;
    private final PermissionGroupResolver resolver;
    private final LuckPermsWrapper lp;
    private final ComponentParser parser;

    public TeamManager(SimplePrefix plugin,
                       LuckPermsWrapper lp,
                       ConfigManager config,
                       GroupManager groups,
                       PermissionGroupResolver resolver) {
        this.plugin = plugin;
        this.lp = lp;
        this.config = config;
        this.groups = groups;
        this.resolver = resolver;
        this.parser = new ComponentParser(plugin);
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void updatePlayer(Player p) {
        if (p == null || !p.isOnline()) return;
        GroupManager.GroupData g = groupOf(p);
        String teamId = teamIdFor(p, g.priority);
        removeCurrentTeam(p);
        Team t = scoreboard.getTeam(teamId);
        if (t == null) t = scoreboard.registerNewTeam(teamId);

        String[] split = splitAroundPlayer(config.getTabFormat());
        String before = replaceTokens(split[0], g, p, true);
        String after = replaceTokens(split[1], g, p, false);

        String prefix = limit(parser.parse(before), PREFIX_LIMIT);
        String suffix = limit(parser.parse(after), SUFFIX_LIMIT);

        if (prefix.trim().isEmpty()) prefix = " ";
        t.setPrefix(prefix);
        t.setSuffix(suffix);

        if (!t.hasEntry(p.getName())) t.addEntry(p.getName());
        if (p.getScoreboard() != scoreboard) p.setScoreboard(scoreboard);
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
    }

    public void removePlayer(Player p) {
        Team t = scoreboard.getPlayerTeam(p);
        if (t == null) return;
        t.removeEntry(p.getName());
        if (t.getSize() == 0) t.unregister();
    }

    private void removeCurrentTeam(Player p) {
        if (p == null || !p.isOnline()) return;

        Team current = scoreboard.getPlayerTeam(p);
        if (current == null) return;

        if (current.hasEntry(p.getName())) {
            current.removeEntry(p.getName());
        }

        boolean isOurTeam = current.getName().startsWith("sp_");
        if (isOurTeam && current.getEntries().isEmpty()) {
            current.unregister();
        }
    }


    private GroupManager.GroupData groupOf(Player p) {
        String g = (lp != null) ? lp.getPrimaryGroup(p) : resolver.resolveGroup(p);
        GroupManager.GroupData data = groups.getGroup(g);
        return data != null ? data : groups.getGroup("default");
    }

    private String teamIdFor(Player p, int priority) {
        String sort = String.format("%03d", Math.max(0, Math.min(999, priority)));
        String tail = p.getName().toLowerCase();
        tail = tail.substring(0, Math.min(8, tail.length()));
        String id = TEAM_NS + "_" + sort + "_" + tail;
        return id.length() <= 16 ? id : id.substring(0, 16);
    }

    private String[] splitAroundPlayer(String format) {
        String f = (format == null || format.isEmpty()) ? "{prefix} {player}" : format;
        int idx = f.indexOf("{player}");
        if (idx < 0) return new String[]{f, ""};
        return new String[]{f.substring(0, idx), f.substring(idx + "{player}".length())};
    }

    private String replaceTokens(String part, GroupManager.GroupData g, Player p, boolean before) {
        String nameColor = (g.nameColor != null && !g.nameColor.isEmpty()) ? "<" + g.nameColor + ">" : "";
        String out = part
                .replace("{prefix}", g.prefix != null ? g.prefix : "")
                .replace("{suffix}", g.suffix != null ? g.suffix : "")
                .replace("{displayname}", p.getDisplayName())
                .replace("{message}", "");
        if (before) out = out + nameColor;
        return out;
    }

    private String limit(String s, int max) {
        if (s == null) return "";
        int count = 0;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                if (count + 2 > max) break;
                b.append(c).append(s.charAt(++i));
                count += 2;
                continue;
            }
            if (count + 1 > max) break;
            b.append(c);
            count++;
        }
        return b.toString();
    }
}
