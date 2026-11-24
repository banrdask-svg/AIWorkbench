package com.aicraft.aiworkbench.item;

import com.aicraft.aiworkbench.ai.ItemDefinitionValidator;
import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.visual.DynamicModelController;
import com.aicraft.aiworkbench.visual.ItemStateService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class CustomItemFactory {
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey versionKey;
    private final JavaPlugin plugin;
    private final ItemStateService stateService;
    private DynamicModelController modelController;

    public CustomItemFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.versionKey = new NamespacedKey(plugin, "version");
        this.stateService = new ItemStateService(plugin);
    }

    public ItemStack create(GeneratedItemDefinition definition) {
        Material material = ItemDefinitionValidator.matchMaterial(definition.material());
        ItemStack item = new ItemStack(material == null ? Material.STICK : material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.name(), definition.rarity().color()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(definition.rarity().name(), definition.rarity().color()));
        for (String line : definition.lore()) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }
        if (!definition.abilities().isEmpty()) {
            lore.add(Component.empty());
            definition.abilities().forEach(ability -> lore.add(Component.text(
                    "Способность: " + ability.type().name() + " | " + ability.cooldown() + " сек.", NamedTextColor.AQUA)));
        }
        meta.lore(lore);
        meta.setItemModel(new NamespacedKey(plugin, "generated/" + definition.id() + "/base"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemIdKey, PersistentDataType.STRING, definition.id());
        if (definition.owner() != null) {
            pdc.set(ownerKey, PersistentDataType.STRING, definition.owner().toString());
        }
        pdc.set(versionKey, PersistentDataType.STRING, plugin.getPluginMeta().getVersion());
        item.setItemMeta(meta);
        if (modelController != null) {
            modelController.refresh(item, definition);
        }
        return item;
    }

    public String readItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    public ItemStateService stateService() {
        return stateService;
    }

    public void setModelController(DynamicModelController modelController) {
        this.modelController = modelController;
    }
}
