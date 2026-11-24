package com.aicraft.aiworkbench.visual;

import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.ai.model.VisualStateDefinition;

import java.util.List;
import java.util.Locale;

public final class VisualStateResolver {
    public VisualStateDefinition resolve(GeneratedItemDefinition definition, ItemRuntimeState runtimeState) {
        List<VisualStateDefinition> states = definition.visualKit() == null ? List.of() : definition.visualKit().states();
        if (states == null || states.isEmpty()) {
            return new VisualStateDefinition("base", "default", fallbackPrompt(definition), "HANDHELD", null);
        }
        for (VisualStateDefinition state : states) {
            if (matches(state.condition(), runtimeState)) {
                return state;
            }
        }
        return states.getFirst();
    }

    private boolean matches(String condition, ItemRuntimeState state) {
        if (condition == null || condition.isBlank() || "default".equalsIgnoreCase(condition)) {
            return true;
        }
        String normalized = condition.toLowerCase(Locale.ROOT).replace(" ", "");
        if (normalized.contains("&&")) {
            for (String part : normalized.split("&&")) {
                if (!matches(part, state)) {
                    return false;
                }
            }
            return true;
        }
        if (normalized.contains("==")) {
            String[] parts = normalized.split("==", 2);
            return state.get(parts[0]).equalsIgnoreCase(parts[1].replace("\"", ""));
        }
        return compare(normalized, ">=", state) || compare(normalized, "<=", state)
                || compare(normalized, ">", state) || compare(normalized, "<", state);
    }

    private boolean compare(String expression, String operator, ItemRuntimeState state) {
        if (!expression.contains(operator)) {
            return false;
        }
        String[] parts = expression.split(java.util.regex.Pattern.quote(operator), 2);
        double left = state.number(parts[0]);
        double right;
        try {
            right = Double.parseDouble(parts[1]);
        } catch (NumberFormatException exception) {
            return false;
        }
        return switch (operator) {
            case ">=" -> left >= right;
            case "<=" -> left <= right;
            case ">" -> left > right;
            case "<" -> left < right;
            default -> false;
        };
    }

    private String fallbackPrompt(GeneratedItemDefinition definition) {
        return definition.texture() == null ? definition.name() : definition.texture().description();
    }
}
