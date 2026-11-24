package com.aicraft.aiworkbench.ai.model;

public record VisualAnimationDefinition(
        String type,
        int frameCount,
        int frameTime,
        boolean interpolate,
        double speed
) {
}
