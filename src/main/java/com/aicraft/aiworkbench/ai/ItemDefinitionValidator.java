package com.aicraft.aiworkbench.ai;

import com.aicraft.aiworkbench.ai.model.AbilityDefinition;
import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ItemDefinitionValidator {
    private final PluginConfig config;

    public ItemDefinitionValidator(PluginConfig config) {
        this.config = config;
    }

    public List<String> validate(GeneratedItemDefinition definition) {
        return validate(definition, Set.of());
    }

    public List<String> validate(GeneratedItemDefinition definition, Set<String> customItemIds) {
        List<String> errors = new ArrayList<>();
        if (definition == null) {
            return List.of("Definition is empty");
        }
        validateText(errors, "id", definition.id(), 48);
        if (definition.id() != null && !definition.id().matches("[a-z0-9_\\-]+")) {
            errors.add("id must be lowercase safe text");
        }
        validateText(errors, "name", definition.name(), 64);
        Material material = matchMaterial(definition.material());
        if (material == null || !material.isItem()) {
            errors.add("material is not a valid item");
        }
        if (definition.rarity() == null) {
            errors.add("rarity is missing");
        }
        if (definition.customModelData() < 1 || definition.customModelData() > 999999) {
            errors.add("customModelData is out of range");
        }
        if (definition.lore() != null) {
            definition.lore().forEach(line -> validateText(errors, "lore", line, 96));
        }
        validateMechanic(errors, definition);
        validateVisualKit(errors, definition);
        validateRecipe(errors, definition, customItemIds);
        validateAbilities(errors, definition.abilities());
        if (definition.visual() != null) {
            if (definition.visual().particles() != null) {
                try {
                    Particle.valueOf(definition.visual().particles().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    errors.add("particle is invalid");
                }
            }
            if (definition.visual().sound() != null) {
                try {
                    if (!isKnownSound(definition.visual().sound())) {
                        errors.add("sound is invalid");
                    }
                } catch (IllegalArgumentException exception) {
                    errors.add("sound is invalid");
                }
            }
        }
        if (definition.texture() != null) {
            validateText(errors, "texture.description", definition.texture().description(), 256);
            String fileName = definition.texture().fileName();
            if (fileName != null && !fileName.matches("[a-z0-9_\\-]+")) {
                errors.add("texture.fileName must be safe");
            }
        }
        return errors;
    }

    private void validateMechanic(List<String> errors, GeneratedItemDefinition definition) {
        if (definition.mechanic() == null) {
            errors.add("mechanic is missing");
            return;
        }
        if (definition.mechanic().complexity() < 1 || definition.mechanic().complexity() > 10) {
            errors.add("mechanic complexity is out of range");
        }
        if (definition.mechanic().states() != null) {
            definition.mechanic().states().forEach(state -> {
                validateText(errors, "mechanic.state.id", state.id(), 48);
                if (state.max() < state.min()) {
                    errors.add("mechanic state range is invalid");
                }
            });
        }
    }

    private void validateVisualKit(List<String> errors, GeneratedItemDefinition definition) {
        if (definition.visualKit() == null || definition.visualKit().states() == null || definition.visualKit().states().isEmpty()) {
            errors.add("visualKit states are missing");
            return;
        }
        if (definition.visualKit().states().size() > config.maxTexturesPerItem()) {
            errors.add("too many visual states");
        }
        definition.visualKit().states().forEach(state -> {
            validateText(errors, "visualState.id", state.id(), 48);
            validateText(errors, "visualState.texturePrompt", state.texturePrompt(), 512);
            if (state.animation() != null && state.animation().frameCount() > config.maxAnimationFrames()) {
                errors.add("animation frame count is too high");
            }
        });
    }

    private boolean isKnownSound(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return Registry.SOUNDS.keyStream()
                .map(key -> key.getKey().toLowerCase(Locale.ROOT))
                .anyMatch(key -> key.equals(normalized) || key.replace('.', '_').equals(normalized));
    }

    private void validateRecipe(List<String> errors, GeneratedItemDefinition definition, Set<String> customItemIds) {
        if (definition.recipe() == null || definition.recipe().ingredients() == null) {
            errors.add("recipe is missing");
            return;
        }
        for (Map.Entry<String, String> ingredient : definition.recipe().ingredients().entrySet()) {
            if (ingredient.getKey().length() != 1 || !isValidRecipeIngredient(ingredient.getValue(), customItemIds)) {
                errors.add("recipe ingredient is invalid");
            }
        }
        if ("SHAPED".equalsIgnoreCase(definition.recipe().type())) {
            if (definition.recipe().shape() == null || definition.recipe().shape().isEmpty() || definition.recipe().shape().size() > 3) {
                errors.add("shaped recipe shape is invalid");
            }
        }
    }

    private void validateAbilities(List<String> errors, List<AbilityDefinition> abilities) {
        if (abilities == null || abilities.size() > 2) {
            errors.add("abilities must contain up to two entries");
            return;
        }
        for (AbilityDefinition ability : abilities) {
            if (ability.type() == null) {
                errors.add("ability type is missing");
            }
            if (ability.cooldown() < config.minCooldownSeconds() || ability.cooldown() > config.maxCooldownSeconds()) {
                errors.add("ability cooldown is out of range");
            }
            if (ability.duration() < 0 || ability.duration() > config.maxDurationSeconds()) {
                errors.add("ability duration is out of range");
            }
            if (ability.power() < 0 || ability.power() > config.maxAbilityPower()) {
                errors.add("ability power is out of range");
            }
            if (ability.radius() < 0 || ability.radius() > config.maxRadius()) {
                errors.add("ability radius is out of range");
            }
        }
    }

    private void validateText(List<String> errors, String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            errors.add(field + " is missing");
            return;
        }
        if (value.length() > maxLength) {
            errors.add(field + " is too long");
        }
    }

    public static Material matchMaterial(String value) {
        if (value == null) {
            return null;
        }
        return Material.matchMaterial(value.toUpperCase(Locale.ROOT));
    }

    public static boolean isCustomIngredient(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith("aiworkbench:");
    }

    public static String customIngredientId(String value) {
        return value.substring("aiworkbench:".length()).toLowerCase(Locale.ROOT);
    }

    private boolean isValidRecipeIngredient(String value, Set<String> customItemIds) {
        if (isCustomIngredient(value)) {
            String id = customIngredientId(value);
            return id.matches("[a-z0-9_\\-]+") && customItemIds.contains(id);
        }
        return matchMaterial(value) != null;
    }
}
