package com.aicraft.aiworkbench.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class Messages {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final YamlConfiguration messages;

    public Messages(JavaPlugin plugin) {
        this.messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public Component get(String key) {
        return miniMessage.deserialize(messages.getString("prefix", "") + messages.getString(key, key));
    }

    public Component get(String key, Map<String, String> placeholders) {
        String value = messages.getString("prefix", "") + messages.getString(key, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return miniMessage.deserialize(value);
    }
}
