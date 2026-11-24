package com.aicraft.aiworkbench.service;

import com.aicraft.aiworkbench.ai.AIService;
import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.config.PluginConfig;
import com.aicraft.aiworkbench.item.CustomItemFactory;
import com.aicraft.aiworkbench.recipe.RecipeManager;
import com.aicraft.aiworkbench.repository.JsonItemRepository;
import com.aicraft.aiworkbench.resourcepack.ResourcePackManager;
import com.aicraft.aiworkbench.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CreationService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final Messages messages;
    private final AIService aiService;
    private final JsonItemRepository repository;
    private final CustomItemFactory itemFactory;
    private final RecipeManager recipeManager;
    private final ResourcePackManager resourcePackManager;
    private final RateLimitService rateLimitService;
    private final Set<UUID> awaitingPrompt = ConcurrentHashMap.newKeySet();
    private final Map<UUID, GeneratedItemDefinition> previews = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastPrompts = new ConcurrentHashMap<>();
    private BiConsumer<Player, GeneratedItemDefinition> previewOpener = (player, definition) -> setPreview(player.getUniqueId(), definition);

    public CreationService(JavaPlugin plugin, PluginConfig config, Messages messages, AIService aiService,
                           JsonItemRepository repository, CustomItemFactory itemFactory, RecipeManager recipeManager,
                           ResourcePackManager resourcePackManager, RateLimitService rateLimitService) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.aiService = aiService;
        this.repository = repository;
        this.itemFactory = itemFactory;
        this.recipeManager = recipeManager;
        this.resourcePackManager = resourcePackManager;
        this.rateLimitService = rateLimitService;
    }

    public void awaitPrompt(Player player) {
        awaitingPrompt.add(player.getUniqueId());
        player.sendMessage(messages.get("prompt-request"));
    }

    public boolean isAwaitingPrompt(UUID playerId) {
        return awaitingPrompt.contains(playerId);
    }

    public void acceptPrompt(Player player, String prompt) {
        awaitingPrompt.remove(player.getUniqueId());
        if ("cancel".equalsIgnoreCase(prompt.trim()) || "отмена".equalsIgnoreCase(prompt.trim())) {
            player.sendMessage(messages.get("prompt-cancelled"));
            return;
        }
        generate(player, prompt);
    }

    public void generate(Player player, String prompt) {
        if (prompt.length() > config.promptMaxLength()) {
            player.sendMessage(messages.get("prompt-too-long"));
            return;
        }
        if (!rateLimitService.tryAcquire(player.getUniqueId())) {
            player.sendMessage(messages.get("generation-rate-limited"));
            return;
        }
        lastPrompts.put(player.getUniqueId(), prompt);
        player.sendMessage(messages.get("generation-started"));
        aiService.generateItem(prompt, repository.items().stream()
                .filter(item -> Objects.equals(item.owner(), player.getUniqueId()))
                .map(GeneratedItemDefinition::id)
                .toList()).whenComplete((definition, throwable) -> {
            rateLimitService.release();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable != null) {
                    player.sendMessage(messages.get("generation-failed"));
                    return;
                }
                GeneratedItemDefinition approved = definition.approve(player.getUniqueId(), prompt);
                setPreview(player.getUniqueId(), approved);
                player.sendMessage(messages.get("preview-ready"));
                previewOpener.accept(player, approved);
            });
        });
    }

    public void setPreview(UUID playerId, GeneratedItemDefinition definition) {
        previews.put(playerId, definition);
    }

    public void confirm(Player player) {
        GeneratedItemDefinition definition = previews.remove(player.getUniqueId());
        if (definition == null) {
            player.closeInventory();
            return;
        }
        GeneratedItemDefinition pendingDefinition = definition;
        Collection<GeneratedItemDefinition> snapshot = new ArrayList<>(repository.items());
        snapshot.add(pendingDefinition);
        player.closeInventory();
        player.sendMessage(messages.get("resourcepack-building"));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> finishConfirm(player, pendingDefinition, snapshot));
    }

    private void finishConfirm(Player player, GeneratedItemDefinition pendingDefinition, Collection<GeneratedItemDefinition> snapshot) {
        try {
            resourcePackManager.rebuild(snapshot);
            GeneratedItemDefinition savedDefinition = pendingDefinition.withPackVersion(resourcePackManager.packVersion());
            repository.saveItem(savedDefinition);
            Bukkit.getScheduler().runTask(plugin, () -> {
                recipeManager.register(savedDefinition);
                player.getInventory().addItem(itemFactory.create(savedDefinition));
                player.sendMessage(messages.get("item-created"));
            });
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось сохранить созданный предмет: " + exception.getMessage());
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(messages.get("generation-failed")));
        }
    }

    public void regenerate(Player player) {
        generate(player, lastPrompts.getOrDefault(player.getUniqueId(), ""));
    }

    public void regenerateVisual(Player player) {
        GeneratedItemDefinition preview = previews.get(player.getUniqueId());
        String prompt = lastPrompts.getOrDefault(player.getUniqueId(), preview == null ? "" : preview.prompt());
        generate(player, prompt + "\nСохрани совместимую механику, но заново придумай визуальную идентичность, visual states, модели, анимации и ассеты ресурспака. Все видимые тексты оставь на русском.");
    }

    public void cancel(Player player) {
        awaitingPrompt.remove(player.getUniqueId());
        previews.remove(player.getUniqueId());
        player.sendMessage(messages.get("prompt-cancelled"));
    }

    public ItemStack previewItem(GeneratedItemDefinition definition) {
        return itemFactory.create(definition);
    }

    public void setPreviewOpener(BiConsumer<Player, GeneratedItemDefinition> previewOpener) {
        this.previewOpener = previewOpener;
    }
}
