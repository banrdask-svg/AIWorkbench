package com.aicraft.aiworkbench.ai.model;

import java.util.List;

public record MechanicDefinition(
        int complexity,
        List<String> summary,
        List<MechanicStateDefinition> states,
        List<String> triggers,
        List<String> actions,
        List<String> phases,
        List<String> modes,
        List<String> evolution
) {
}
