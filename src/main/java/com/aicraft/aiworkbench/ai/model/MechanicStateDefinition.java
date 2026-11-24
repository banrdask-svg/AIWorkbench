package com.aicraft.aiworkbench.ai.model;

public record MechanicStateDefinition(
        String id,
        String type,
        double min,
        double max,
        String initial
) {
}
