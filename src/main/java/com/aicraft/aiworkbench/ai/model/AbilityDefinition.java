package com.aicraft.aiworkbench.ai.model;

public record AbilityDefinition(
        AbilityType type,
        int cooldown,
        int duration,
        double power,
        double radius
) {
}
