package com.aicraft.aiworkbench.listener;

import com.aicraft.aiworkbench.service.CreationService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class CreationChatListener implements Listener {
    private final Plugin plugin;
    private final CreationService creationService;

    public CreationChatListener(Plugin plugin, CreationService creationService) {
        this.plugin = plugin;
        this.creationService = creationService;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!creationService.isAwaitingPrompt(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String prompt = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> creationService.acceptPrompt(event.getPlayer(), prompt));
    }
}
