package com.aicraft.aiworkbench.gui;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.item.WorkbenchManager;
import com.aicraft.aiworkbench.repository.JsonItemRepository;
import com.aicraft.aiworkbench.resourcepack.ResourcePackManager;
import com.aicraft.aiworkbench.service.CreationService;
import com.aicraft.aiworkbench.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class GuiManager {
    private final JavaPlugin plugin;
    private final Messages messages;
    private final CreationService creationService;
    private final JsonItemRepository repository;
    private final WorkbenchManager workbenchManager;
    private final ResourcePackManager resourcePackManager;
    private final GuiItems items;

    public GuiManager(JavaPlugin plugin, Messages messages, CreationService creationService, JsonItemRepository repository,
                      WorkbenchManager workbenchManager, ResourcePackManager resourcePackManager) {
        this.plugin = plugin;
        this.messages = messages;
        this.creationService = creationService;
        this.repository = repository;
        this.workbenchManager = workbenchManager;
        this.resourcePackManager = resourcePackManager;
        this.items = new GuiItems(plugin);
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(new GuiHolder("main"), 27, Component.text("ИИ-верстак", NamedTextColor.AQUA));
        fill(inventory);
        inventory.setItem(10, items.button(Material.WRITABLE_BOOK, Component.text("Создать предмет", NamedTextColor.GREEN), GuiAction.CREATE_ITEM,
                List.of(Component.text("Опиши идею в чате", NamedTextColor.GRAY))));
        inventory.setItem(12, items.button(Material.AMETHYST_SHARD, Component.text("Случайная идея", NamedTextColor.LIGHT_PURPLE), GuiAction.RANDOM_ITEM,
                List.of(Component.text("ИИ сам придумает предмет", NamedTextColor.GRAY))));
        inventory.setItem(14, items.button(Material.BOOKSHELF, Component.text("Библиотека", NamedTextColor.YELLOW), GuiAction.OPEN_LIBRARY,
                List.of(Component.text("Созданные предметы", NamedTextColor.GRAY))));
        inventory.setItem(16, items.button(Material.MAP, Component.text("Ресурспак", NamedTextColor.AQUA), GuiAction.RESOURCEPACK_REBUILD,
                List.of(Component.text("Пересобрать сгенерированный пак", NamedTextColor.GRAY))));
        player.openInventory(inventory);
    }

    public void openPreview(Player player, GeneratedItemDefinition definition) {
        creationService.setPreview(player.getUniqueId(), definition);
        Inventory inventory = Bukkit.createInventory(new GuiHolder("preview"), 45, Component.text("Предпросмотр предмета", NamedTextColor.AQUA));
        fill(inventory);
        inventory.setItem(13, creationService.previewItem(definition));
        inventory.setItem(20, items.button(Material.COMPARATOR, Component.text("Механика", NamedTextColor.AQUA), GuiAction.CLOSE, mechanicLore(definition)));
        inventory.setItem(24, items.button(Material.GLOW_INK_SAC, Component.text("Визуальный набор", NamedTextColor.LIGHT_PURPLE), GuiAction.CLOSE, visualLore(definition)));
        inventory.setItem(29, items.button(Material.LIME_DYE, Component.text("Создать", NamedTextColor.GREEN), GuiAction.CONFIRM_CREATE, List.of()));
        inventory.setItem(31, items.button(Material.CLOCK, Component.text("Перегенерировать", NamedTextColor.YELLOW), GuiAction.REGENERATE, List.of()));
        inventory.setItem(32, items.button(Material.PAINTING, Component.text("Перегенерировать вид", NamedTextColor.AQUA), GuiAction.REGENERATE_VISUAL, List.of(Component.text("Сохранить механику, обновить внешний вид", NamedTextColor.GRAY))));
        inventory.setItem(33, items.button(Material.RED_DYE, Component.text("Отмена", NamedTextColor.RED), GuiAction.CANCEL, List.of()));
        player.openInventory(inventory);
    }

    public void openLibrary(Player player) {
        Inventory inventory = Bukkit.createInventory(new GuiHolder("library"), 54, Component.text("ИИ-библиотека", NamedTextColor.YELLOW));
        fill(inventory);
        Collection<GeneratedItemDefinition> all = repository.items();
        if (all.isEmpty()) {
            inventory.setItem(22, items.button(Material.BARRIER, Component.text("Библиотека пуста", NamedTextColor.RED), GuiAction.BACK,
                    List.of(Component.text("Создай первый предмет", NamedTextColor.GRAY))));
        } else {
            int slot = 0;
            for (GeneratedItemDefinition definition : all) {
                if (slot >= 45) {
                    break;
                }
                inventory.setItem(slot++, creationService.previewItem(definition));
            }
        }
        inventory.setItem(49, items.button(Material.ARROW, Component.text("Назад", NamedTextColor.WHITE), GuiAction.BACK, List.of()));
        player.openInventory(inventory);
    }

    public void handle(Player player, ItemStack clicked) {
        GuiAction action = items.action(clicked);
        if (action == null) {
            return;
        }
        switch (action) {
            case CREATE_ITEM -> {
                player.closeInventory();
                creationService.awaitPrompt(player);
            }
            case RANDOM_ITEM -> {
                player.closeInventory();
                creationService.generate(player, "");
            }
            case OPEN_LIBRARY -> openLibrary(player);
            case CONFIRM_CREATE -> creationService.confirm(player);
            case REGENERATE -> {
                player.closeInventory();
                creationService.regenerate(player);
            }
            case REGENERATE_VISUAL -> {
                player.closeInventory();
                creationService.regenerateVisual(player);
            }
            case CANCEL, CLOSE -> {
                creationService.cancel(player);
                player.closeInventory();
            }
            case BACK -> openMain(player);
            case RESOURCEPACK_REBUILD -> {
                try {
                    player.sendMessage(messages.get("resourcepack-built", Map.of("file", resourcePackManager.rebuild(repository.items()).toString())));
                } catch (IOException exception) {
                    player.sendMessage(Component.text("Не удалось пересобрать ресурспак.", NamedTextColor.RED));
                    plugin.getLogger().warning("Resource pack rebuild failed: " + exception.getMessage());
                }
            }
        }
    }

    private void fill(Inventory inventory) {
        ItemStack filler = items.filler();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private List<Component> mechanicLore(GeneratedItemDefinition definition) {
        List<Component> lore = new ArrayList<>();
        if (definition.mechanic() == null) {
            return List.of(Component.text("Нет данных механики", NamedTextColor.GRAY));
        }
        lore.add(Component.text("Сложность: " + definition.mechanic().complexity() + "/10", NamedTextColor.YELLOW));
        if (definition.mechanic().summary() != null) {
            definition.mechanic().summary().stream().limit(5).forEach(line -> lore.add(Component.text(line, NamedTextColor.GRAY)));
        }
        if (definition.mechanic().states() != null) {
            lore.add(Component.text("Состояния: " + definition.mechanic().states().size(), NamedTextColor.AQUA));
        }
        if (definition.mechanic().modes() != null && !definition.mechanic().modes().isEmpty()) {
            lore.add(Component.text("Режимы: " + String.join(", ", definition.mechanic().modes()), NamedTextColor.LIGHT_PURPLE));
        }
        return lore;
    }

    private List<Component> visualLore(GeneratedItemDefinition definition) {
        List<Component> lore = new ArrayList<>();
        if (definition.visualKit() == null) {
            return List.of(Component.text("Нет визуального набора", NamedTextColor.GRAY));
        }
        lore.add(Component.text("Тема: " + definition.visualKit().visualTheme(), NamedTextColor.GRAY));
        int states = definition.visualKit().states() == null ? 0 : definition.visualKit().states().size();
        lore.add(Component.text("Сгенерировано ассетов: " + states, NamedTextColor.AQUA));
        lore.add(Component.text("Нужна версия пака: v" + definition.requiredPackVersion(), NamedTextColor.YELLOW));
        if (definition.visualKit().states() != null) {
            definition.visualKit().states().stream().limit(5)
                    .forEach(state -> lore.add(Component.text(state.id() + " -> " + state.condition(), NamedTextColor.DARK_AQUA)));
        }
        return lore;
    }
}
