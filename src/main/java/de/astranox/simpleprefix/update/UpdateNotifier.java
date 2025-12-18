package de.astranox.simpleprefix.update;

import de.astranox.simpleprefix.SimplePrefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UpdateNotifier {

    private final SimplePrefix plugin;
    private final MiniMessage mm;
    private final LegacyComponentSerializer serializer;
    private boolean consoleNotified = false;
    private final Set<UUID> notifiedPlayers = new HashSet<>();

    public UpdateNotifier(SimplePrefix plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
        this.serializer = LegacyComponentSerializer.legacySection();
    }

    public void notifyConsole(String currentVersion, String latestVersion, String downloadUrl) {
        if (consoleNotified) return;
        consoleNotified = true;

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            plugin.getLogger().info("§aUpdate available for SimplePrefix!");
            plugin.getLogger().info("§eCurrent: §7" + currentVersion + " §e→ Latest: §a" + latestVersion);
            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                plugin.getLogger().info("§eDownload: §b" + downloadUrl);
            }
            plugin.getLogger().info("§eRun §b/sp update install §eto download");
            plugin.getLogger().info("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        });
    }

    public void notifyPlayer(Player player, String currentVersion, String latestVersion) {
        if (!player.hasPermission("simpleprefix.notify")) return;
        if (notifiedPlayers.contains(player.getUniqueId())) return;

        notifiedPlayers.add(player.getUniqueId());

        Component header = mm.deserialize("<dark_gray><st>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</st>");

        Component title = mm.deserialize("<gold><bold>SimplePrefix</bold> <dark_gray>» <yellow>Update available!");

        Component versionInfo = mm.deserialize(
                "<yellow>Current: <gray>" + currentVersion + " <yellow>→ Latest: <green>" + latestVersion
        );

        Component installButton = mm.deserialize("<green><bold>[Install]</bold>")
                .clickEvent(ClickEvent.runCommand("/sp update install"))
                .hoverEvent(HoverEvent.showText(
                        mm.deserialize("<gray>Download update\n<dark_gray>Requires restart to apply")
                ));

        Component hotReloadButton = mm.deserialize("<red><bold>[Hot-Reload]</bold>")
                .clickEvent(ClickEvent.runCommand("/sp update install --hot"))
                .hoverEvent(HoverEvent.showText(
                        mm.deserialize("<red>⚠ Install and reload immediately\n<dark_gray>Experimental - may cause issues")
                ));

        Component infoButton = mm.deserialize("<aqua>[Info]")
                .clickEvent(ClickEvent.runCommand("/sp update info"))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<gray>Show update details")));

        Component buttons = installButton
                .append(Component.text("  "))
                .append(hotReloadButton)
                .append(Component.text("  "))
                .append(infoButton);

        sendComponent(player, header);
        sendComponent(player, title);
        sendComponent(player, versionInfo);
        player.sendMessage("");
        sendComponent(player, buttons);
        sendComponent(player, header);
    }

    private void sendComponent(Player player, Component component) {
        try {
            player.sendMessage(BungeeComponentSerializer.get().serialize(component));
        } catch (NoSuchMethodError | Exception e) {

            String legacy = serializer.serialize(component);
            player.sendMessage(legacy);
        }
    }

    public void sendFormattedMessage(Player player, String miniMessageFormat) {
        try {
            Component component = mm.deserialize(miniMessageFormat);
            sendComponent(player, component);
        } catch (Exception e) {

            player.sendMessage(miniMessageFormat);
        }
    }

    public void reset() {
        consoleNotified = false;
        notifiedPlayers.clear();
    }
}