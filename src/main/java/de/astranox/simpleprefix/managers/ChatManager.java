package de.astranox.simpleprefix.managers;

import de.astranox.simpleprefix.SimplePrefix;
import de.astranox.simpleprefix.util.ComponentParser;
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
    private final LuckPermsWrapper luckPermsWrapper;
    private final ConfigManager configManager;
    private final GroupManager groupManager;
    private final PermissionGroupResolver permissionGroupResolver;
    private final ComponentParser componentParser;
    private final boolean useLuckPerms;

    public ChatManager(SimplePrefix plugin, LuckPermsWrapper luckPermsWrapper, ConfigManager configManager,
                       GroupManager groupManager, PermissionGroupResolver permissionGroupResolver,
                       boolean useLuckPerms) {
        this.plugin = plugin;
        this.luckPermsWrapper = luckPermsWrapper;
        this.configManager = configManager;
        this.groupManager = groupManager;
        this.permissionGroupResolver = permissionGroupResolver;
        this.componentParser = new ComponentParser(plugin);
        this.useLuckPerms = useLuckPerms;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        if (!configManager.isChatFormatEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        String primaryGroup;

        if (useLuckPerms && luckPermsWrapper != null) {
            primaryGroup = luckPermsWrapper.getPrimaryGroup(player);
        } else {
            primaryGroup = permissionGroupResolver.resolveGroup(player);
        }

        GroupManager.GroupData groupData = groupManager.getGroup(primaryGroup);

        if (groupData == null) {
            groupData = groupManager.getGroup("default");
            if (groupData == null) {
                return;
            }
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

        String primaryGroup;

        if (useLuckPerms && luckPermsWrapper != null) {
            primaryGroup = luckPermsWrapper.getPrimaryGroup(player);
        } else {
            primaryGroup = permissionGroupResolver.resolveGroup(player);
        }

        GroupManager.GroupData groupData = groupManager.getGroup(primaryGroup);

        if (groupData == null) {
            groupData = groupManager.getGroup("default");
            if (groupData == null) {
                return;
            }
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
