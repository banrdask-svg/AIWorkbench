package com.aicraft.aiworkbench.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class WorkbenchManager {
    private final NamespacedKey workbenchKey;

    public WorkbenchManager(JavaPlugin plugin) {
        this.workbenchKey = new NamespacedKey(plugin, "workbench");
    }

    public ItemStack createWorkbenchItem() {
        ItemStack item = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("ИИ-верстак", NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("Создаёт кастомные предметы через ИИ", NamedTextColor.GRAY),
                Component.text("Поставь и нажми ПКМ, чтобы открыть", NamedTextColor.DARK_AQUA)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(workbenchKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWorkbench(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(workbenchKey, PersistentDataType.BYTE);
    }

    public NamespacedKey workbenchKey() {
        return workbenchKey;
    }
}
