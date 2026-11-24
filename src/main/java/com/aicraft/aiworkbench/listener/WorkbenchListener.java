package com.aicraft.aiworkbench.listener;

import com.aicraft.aiworkbench.gui.GuiManager;
import com.aicraft.aiworkbench.item.WorkbenchManager;
import org.bukkit.GameMode;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorkbenchListener implements Listener {
    private final JavaPlugin plugin;
    private final WorkbenchManager workbenchManager;
    private final GuiManager guiManager;

    public WorkbenchListener(JavaPlugin plugin, WorkbenchManager workbenchManager, GuiManager guiManager) {
        this.plugin = plugin;
        this.workbenchManager = workbenchManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!workbenchManager.isWorkbench(event.getItemInHand())) {
            return;
        }
        if (event.getBlockPlaced().getState() instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(workbenchManager.workbenchKey(), PersistentDataType.BYTE, (byte) 1);
            tileState.update(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }
        if (!(event.getClickedBlock().getState() instanceof TileState tileState)) {
            return;
        }
        if (!tileState.getPersistentDataContainer().has(workbenchManager.workbenchKey(), PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.hasPermission("aiworkbench.use")) {
            guiManager.openMain(player);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof TileState tileState)) {
            return;
        }
        if (!tileState.getPersistentDataContainer().has(workbenchManager.workbenchKey(), PersistentDataType.BYTE)) {
            return;
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), workbenchManager.createWorkbenchItem());
    }
}
