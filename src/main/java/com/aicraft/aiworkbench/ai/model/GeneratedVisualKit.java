package com.aicraft.aiworkbench.ai.model;

import java.util.List;

public record GeneratedVisualKit(
        VisualIdentityDefinition identity,
        String visualTheme,
        List<VisualStateDefinition> states,
        List<VisualBindingDefinition> bindings,
        String item,
        String held,
        String projectile,
        String zone,
        String summon,
        String placed
) {
}
