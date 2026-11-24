package com.aicraft.aiworkbench.visual;

import java.util.HashMap;
import java.util.Map;

public final class ItemRuntimeState {
    private final Map<String, String> values = new HashMap<>();

    public String get(String key) {
        return values.getOrDefault(key, "0");
    }

    public double number(String key) {
        try {
            return Double.parseDouble(get(key));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public void put(String key, String value) {
        values.put(key, value);
    }

    public Map<String, String> values() {
        return Map.copyOf(values);
    }
}
