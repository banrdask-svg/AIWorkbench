package com.aicraft.aiworkbench.ai.model;

import java.util.List;

public record VisualIdentityDefinition(
        String identity,
        String silhouette,
        List<String> materials,
        List<String> palette,
        List<String> importantFeatures,
        List<String> forbiddenChanges
) {
}
