package com.aicraft.aiworkbench.visual;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class DynamicModelController {
    private final JavaPlugin plugin;
    private final ItemStateService stateService;
    private final VisualStateResolver resolver = new VisualStateResolver();

    public DynamicModelController(JavaPlugin plugin, ItemStateService stateService) {
        this.plugin = plugin;
        this.stateService = stateService;
    }

    public void refresh(ItemStack item, GeneratedItemDefinition definition) {
        VisualStateDefinition visualState = resolver.resolve(definition, stateService.read(item));
        String modelId = sanitize(definition.id()) + "/" + sanitize(visualState.id());
        if (modelId.equals(stateService.visualState(item))) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey(plugin, "generated/" + modelId));
        item.setItemMeta(meta);
        stateService.visualState(item, modelId);
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? "base" : value.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
    }
}
