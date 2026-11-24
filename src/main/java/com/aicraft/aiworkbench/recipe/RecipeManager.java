package com.aicraft.aiworkbench.recipe;

import com.aicraft.aiworkbench.ai.ItemDefinitionValidator;
import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.item.CustomItemFactory;
import com.aicraft.aiworkbench.repository.JsonItemRepository;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public final class RecipeManager {
    private final JavaPlugin plugin;
    private final CustomItemFactory itemFactory;
    private final JsonItemRepository repository;

    public RecipeManager(JavaPlugin plugin, CustomItemFactory itemFactory, JsonItemRepository repository) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.repository = repository;
    }

    public void registerWorkbenchRecipe(ItemStack result) {
        NamespacedKey key = new NamespacedKey(plugin, "ai_workbench");
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("RCR", "ISI", "OOO");
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('C', Material.CRAFTER);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.SMITHING_TABLE);
        recipe.setIngredient('O', Material.OBSIDIAN);
        plugin.getServer().addRecipe(recipe);
    }

    public void registerAll(Collection<GeneratedItemDefinition> definitions) {
        definitions.forEach(this::register);
    }

    public void register(GeneratedItemDefinition definition) {
        if (definition.recipe() == null) {
            return;
        }
        NamespacedKey key = uniqueKey(definition.id());
        if ("SHAPELESS".equalsIgnoreCase(definition.recipe().type())) {
            ShapelessRecipe recipe = new ShapelessRecipe(key, itemFactory.create(definition));
            for (String materialName : definition.recipe().ingredients().values()) {
                RecipeChoice choice = choice(materialName);
                if (choice != null) {
                    recipe.addIngredient(choice);
                }
            }
            plugin.getServer().addRecipe(recipe);
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.create(definition));
        recipe.shape(definition.recipe().shape().toArray(String[]::new));
        for (Map.Entry<String, String> entry : definition.recipe().ingredients().entrySet()) {
            RecipeChoice choice = choice(entry.getValue());
            if (choice != null) {
                recipe.setIngredient(entry.getKey().charAt(0), choice);
            }
        }
        plugin.getServer().addRecipe(recipe);
    }

    private RecipeChoice choice(String value) {
        if (ItemDefinitionValidator.isCustomIngredient(value)) {
            return repository.find(ItemDefinitionValidator.customIngredientId(value))
                    .map(definition -> new RecipeChoice.ExactChoice(itemFactory.create(definition)))
                    .orElse(null);
        }
        Material material = ItemDefinitionValidator.matchMaterial(value);
        return material == null ? null : new RecipeChoice.MaterialChoice(material);
    }

    private NamespacedKey uniqueKey(String id) {
        String safe = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        NamespacedKey key = new NamespacedKey(plugin, "item_" + safe);
        int index = 1;
        while (plugin.getServer().getRecipe(key) != null) {
            key = new NamespacedKey(plugin, "item_" + safe + "_" + index++);
        }
        return key;
    }
}
