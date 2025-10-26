package de.astranox.simpleprefix.util;

import de.astranox.simpleprefix.SimplePrefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ComponentParser {
    private final SimplePrefix plugin;
    private final MiniMessage miniMessage;

    public ComponentParser(SimplePrefix plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public String parse(String text) {
        if (text == null || text.isEmpty()) return "";

        try {
            Component component = miniMessage.deserialize(text);
            String legacy = LegacyComponentSerializer.legacySection().serialize(component);

            if (VersionUtil.isLegacyVersion()) {

                legacy = legacy.replaceAll("(?i)(§x(§[0-9A-F]){6})", "");
                legacy = legacy.replaceAll("(?i)§[lmnok]", "");
            }

            return BukkitColor.apply(legacy);
        } catch (Exception e) {
            plugin.getLogger().warning("ComponentParser error for: " + text);
            e.printStackTrace();
            return text.replaceAll("<[^>]*>", "");
        }
    }
}