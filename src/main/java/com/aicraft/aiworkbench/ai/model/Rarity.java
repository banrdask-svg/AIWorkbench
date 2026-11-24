package com.aicraft.aiworkbench.ai.model;

import net.kyori.adventure.text.format.NamedTextColor;

public enum Rarity {
    COMMON(NamedTextColor.WHITE),
    UNCOMMON(NamedTextColor.GREEN),
    RARE(NamedTextColor.AQUA),
    EPIC(NamedTextColor.LIGHT_PURPLE),
    LEGENDARY(NamedTextColor.GOLD);

    private final NamedTextColor color;

    Rarity(NamedTextColor color) {
        this.color = color;
    }

    public NamedTextColor color() {
        return color;
    }
}
