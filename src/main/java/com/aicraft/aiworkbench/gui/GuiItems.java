package com.aicraft.aiworkbench.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class GuiItems {
    private final NamespacedKey actionKey;

    public GuiItems(JavaPlugin plugin) {
        this.actionKey = new NamespacedKey(plugin, "gui_action");
    }

    public ItemStack button(Material material, Component name, GuiAction action, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action.name());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }

    public GuiAction action(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return GuiAction.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
