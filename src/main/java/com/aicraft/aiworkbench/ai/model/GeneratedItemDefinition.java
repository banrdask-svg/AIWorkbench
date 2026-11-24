package com.aicraft.aiworkbench.ai.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GeneratedItemDefinition(
        String id,
        String name,
        String material,
        Rarity rarity,
        int customModelData,
        List<String> lore,
        MechanicDefinition mechanic,
        RecipeDefinition recipe,
        List<AbilityDefinition> abilities,
        VisualDefinition visual,
        TextureDefinition texture,
        GeneratedVisualKit visualKit,
        int assetVersion,
        int requiredPackVersion,
        UUID owner,
        String prompt,
        Instant createdAt,
        boolean approved,
        int uses
) {
    public GeneratedItemDefinition approve(UUID ownerId, String sourcePrompt) {
        return new GeneratedItemDefinition(
                id,
                name,
                material,
                rarity,
                customModelData,
                lore == null ? List.of() : List.copyOf(lore),
                mechanic,
                recipe,
                abilities == null ? List.of() : List.copyOf(abilities),
                visual,
                texture,
                visualKit,
                assetVersion,
                requiredPackVersion,
                ownerId,
                sourcePrompt,
                Instant.now(),
                true,
                uses
        );
    }

    public GeneratedItemDefinition withPackVersion(int packVersion) {
        return new GeneratedItemDefinition(
                id,
                name,
                material,
                rarity,
                customModelData,
                lore == null ? List.of() : List.copyOf(lore),
                mechanic,
                recipe,
                abilities == null ? List.of() : List.copyOf(abilities),
                visual,
                texture,
                visualKit,
                packVersion,
                packVersion,
                owner,
                prompt,
                createdAt,
                approved,
                uses
        );
    }
}
