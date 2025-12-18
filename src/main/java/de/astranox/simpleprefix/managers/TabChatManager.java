package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.util.ComponentParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class TabChatManager implements Listener {

    private final SimplePrefix plugin;
    private final ConfigManager config;
    private final GroupManager groups;
    private final PermissionGroupResolver resolver;
    private final LuckPermsWrapper lp;
    private final ComponentParser parser;

    public TabChatManager(SimplePrefix plugin,
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
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!config.isChatFormatEnabled()) return;
        Player p = e.getPlayer();
        GroupManager.GroupData g = groupOf(p);

        String format = config.getChatFormat(); // z.B. "{prefix} {player} » {message}"
        String nameColor = (g.nameColor != null && !g.nameColor.isEmpty()) ? "<" + g.nameColor + ">" : "";

        String rendered = format
                .replace("{prefix}", g.prefix != null ? g.prefix : "")
                .replace("{suffix}", g.suffix != null ? g.suffix : "")
                .replace("{player}", nameColor + p.getName())
                .replace("{displayname}", nameColor + p.getDisplayName())
                .replace("{message}", "%2$s");

        e.setFormat(parser.parse(rendered));
    }

    public void updateTabEntry(Player p) {
        if (!config.isTabFormatEnabled()) return;
        GroupManager.GroupData g = groupOf(p);
        String[] split = splitAroundPlayer(config.getTabFormat());
        String nameColor = (g.nameColor != null && !g.nameColor.isEmpty()) ? "<" + g.nameColor + ">" : "";

        String before = split[0]
                .replace("{prefix}", g.prefix != null ? g.prefix : "")
                .replace("{suffix}", g.suffix != null ? g.suffix : "");

        String after = split[1]
                .replace("{prefix}", "")
                .replace("{suffix}", g.suffix != null ? g.suffix : "");

        String listName = parser.parse(before + nameColor + p.getName() + after);
        p.setPlayerListName(listName);
    }

    private GroupManager.GroupData groupOf(Player p) {
        String g = (lp != null) ? lp.getPrimaryGroup(p) : resolver.resolveGroup(p);
        GroupManager.GroupData data = groups.getGroup(g);
        return data != null ? data : groups.getGroup("default");
    }

    private String[] splitAroundPlayer(String format) {
        String f = (format == null || format.isEmpty()) ? "{prefix} {player}" : format;
        int idx = f.indexOf("{player}");
        if (idx < 0) return new String[]{f, ""};
        return new String[]{f.substring(0, idx), f.substring(idx + "{player}".length())};
    }
}
