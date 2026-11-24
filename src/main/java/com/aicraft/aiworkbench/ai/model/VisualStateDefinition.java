package com.aicraft.aiworkbench.ai.model;

public record VisualStateDefinition(
        String id,
        String condition,
        String texturePrompt,
        String modelType,
        VisualAnimationDefinition animation
) {
}
