package de.syntaxjasom.simplePrefix.util;

import de.syntaxjasom.simplePrefix.SimplePrefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ComponentParser {

    private final SimplePrefix plugin;
    private final MiniMessage miniMessage;

    public ComponentParser(SimplePrefix plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        try {
            text = text.replaceAll("<color:(#[0-9A-Fa-f]{6})>", "<$1>");
            text = text.replaceAll("<colour:(#[0-9A-Fa-f]{6})>", "<$1>");

            return miniMessage.deserialize(text);

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing formatting: " + text);
            e.printStackTrace();
            return Component.text(text.replaceAll("<[^>]*>", ""));
        }
    }
}
