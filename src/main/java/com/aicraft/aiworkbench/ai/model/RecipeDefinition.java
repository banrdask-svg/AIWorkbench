package com.aicraft.aiworkbench.ai.model;

import java.util.List;
import java.util.Map;

public record RecipeDefinition(
        String type,
        List<String> shape,
        Map<String, String> ingredients
) {
}
