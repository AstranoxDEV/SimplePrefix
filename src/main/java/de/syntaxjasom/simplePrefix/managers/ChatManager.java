package de.syntaxjasom.simplePrefix.managers;

import de.syntaxjasom.simplePrefix.SimplePrefix;
import de.syntaxjasom.simplePrefix.util.ComponentParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatManager implements Listener {

    private final SimplePrefix plugin;
    private final LuckPerms luckPerms;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final ComponentParser componentParser;

    public ChatManager(SimplePrefix plugin, LuckPerms luckPerms, ConfigManager configManager, GroupManager groupManager) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.componentParser = new ComponentParser(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        if (!configManager.isChatFormatEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);

        if (user == null) {
            return;
        }

        String primaryGroup = user.getPrimaryGroup();

        GroupManager.GroupData groupData = groupManager.getGroup(primaryGroup);

        if (groupData == null) {
            return;
        }

        String prefix = groupData.prefix;
        String suffix = groupData.suffix;
        String message = LegacyComponentSerializer.legacySection().serialize(event.message());

        String format = configManager.getChatFormat();

        String nameColor = "";
        if (groupData.nameColor != null && !groupData.nameColor.isEmpty()) {
            nameColor = "<" + groupData.nameColor + ">";
        }

        format = format.replace("{prefix}", prefix != null ? prefix : "");
        format = format.replace("{suffix}", suffix != null ? suffix : "");
        format = format.replace("{player}", nameColor + player.getName());
        format = format.replace("{displayname}", nameColor + player.displayName().toString());
        format = format.replace("{message}", message);

        Component formattedMessage = componentParser.parse(format);

        event.renderer((source, sourceDisplayName, msg, viewer) -> formattedMessage);
    }

    public void updatePlayerListName(Player player) {
        if (!configManager.isTabFormatEnabled()) {
            player.playerListName(Component.text(player.getName()));
            return;
        }

        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);

        if (user == null) {
            return;
        }

        String primaryGroup = user.getPrimaryGroup();

        GroupManager.GroupData groupData = groupManager.getGroup(primaryGroup);

        if (groupData == null) {
            return;
        }

        String prefix = groupData.prefix;
        String suffix = groupData.suffix;

        String format = configManager.getTabFormat();

        String nameColor = "";
        if (groupData.nameColor != null && !groupData.nameColor.isEmpty()) {
            nameColor = "<" + groupData.nameColor + ">";
        }

        format = format.replace("{prefix}", prefix != null ? prefix : "");
        format = format.replace("{suffix}", suffix != null ? suffix : "");
        format = format.replace("{player}", nameColor + player.getName());
        format = format.replace("{displayname}", nameColor + player.displayName().toString());

        Component tabName = componentParser.parse(format);
        player.playerListName(tabName);
    }

    public Component parseFormatting(String text) {
        return componentParser.parse(text);
    }
}
