package com.aicraft.aiworkbench.ability;

import com.aicraft.aiworkbench.ai.model.AbilityDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface AbilityExecutor {
    void execute(Player player, LivingEntity target, AbilityDefinition ability);
}
