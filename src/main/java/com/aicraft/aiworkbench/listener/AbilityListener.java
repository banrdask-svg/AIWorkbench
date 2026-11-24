package com.aicraft.aiworkbench.listener;

import com.aicraft.aiworkbench.ability.AbilityRegistry;
import com.aicraft.aiworkbench.ai.model.GeneratedItemDefinition;
import com.aicraft.aiworkbench.item.CustomItemFactory;
import com.aicraft.aiworkbench.repository.JsonItemRepository;
import com.aicraft.aiworkbench.visual.DynamicModelController;
import com.aicraft.aiworkbench.visual.ItemStateService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class AbilityListener implements Listener {
    private final CustomItemFactory itemFactory;
    private final JsonItemRepository repository;
    private final AbilityRegistry registry;
    private final ItemStateService stateService;
    private final DynamicModelController modelController;

    public AbilityListener(CustomItemFactory itemFactory, JsonItemRepository repository, AbilityRegistry registry,
                           ItemStateService stateService, DynamicModelController modelController) {
        this.itemFactory = itemFactory;
        this.repository = repository;
        this.registry = registry;
        this.stateService = stateService;
        this.modelController = modelController;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        String itemId = itemFactory.readItemId(player.getInventory().getItemInMainHand());
        if (itemId == null) {
            return;
        }
        repository.find(itemId).ifPresent(definition -> {
            stateService.addCharge(player.getInventory().getItemInMainHand(), 10);
            definition.abilities().forEach(ability -> registry.execute(player, target, ability));
            modelController.refresh(player.getInventory().getItemInMainHand(), definition);
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String itemId = itemFactory.readItemId(player.getInventory().getItemInMainHand());
        if (itemId == null) {
            return;
        }
        LivingEntity target = player.getWorld().getNearbyLivingEntities(player.getLocation(), 6).stream()
                .filter(entity -> !entity.equals(player))
                .findFirst()
                .orElse(player);
        repository.find(itemId).ifPresent(definition -> {
            if (player.isSneaking()) {
                stateService.cycleMode(player.getInventory().getItemInMainHand());
            } else {
                stateService.addCharge(player.getInventory().getItemInMainHand(), 5);
            }
            definition.abilities().forEach(ability -> registry.execute(player, target, ability));
            modelController.refresh(player.getInventory().getItemInMainHand(), definition);
        });
    }
}
