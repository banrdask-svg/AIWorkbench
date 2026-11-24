package com.aicraft.aiworkbench.visual;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemStateService {
    private final NamespacedKey chargeKey;
    private final NamespacedKey modeKey;
    private final NamespacedKey evolutionKey;
    private final NamespacedKey visualStateKey;

    public ItemStateService(JavaPlugin plugin) {
        this.chargeKey = new NamespacedKey(plugin, "state_charge");
        this.modeKey = new NamespacedKey(plugin, "state_mode");
        this.evolutionKey = new NamespacedKey(plugin, "state_evolution");
        this.visualStateKey = new NamespacedKey(plugin, "visual_state");
    }

    public ItemRuntimeState read(ItemStack item) {
        ItemRuntimeState state = new ItemRuntimeState();
        if (item == null || !item.hasItemMeta()) {
            return state;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        state.put("charge", String.valueOf(pdc.getOrDefault(chargeKey, PersistentDataType.INTEGER, 0)));
        state.put("energy", state.get("charge"));
        state.put("mode", pdc.getOrDefault(modeKey, PersistentDataType.STRING, "DEFAULT"));
        state.put("evolution", String.valueOf(pdc.getOrDefault(evolutionKey, PersistentDataType.INTEGER, 0)));
        return state;
    }

    public void addCharge(ItemStack item, int amount) {
        mutate(item, meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            int current = pdc.getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);
            pdc.set(chargeKey, PersistentDataType.INTEGER, Math.max(0, Math.min(100, current + amount)));
        });
    }

    public void cycleMode(ItemStack item) {
        mutate(item, meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String current = pdc.getOrDefault(modeKey, PersistentDataType.STRING, "DEFAULT");
            String next = switch (current) {
                case "PULL" -> "PUSH";
                case "PUSH" -> "ORBIT";
                default -> "PULL";
            };
            pdc.set(modeKey, PersistentDataType.STRING, next);
        });
    }

    public String visualState(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(visualStateKey, PersistentDataType.STRING, "");
    }

    public void visualState(ItemStack item, String value) {
        mutate(item, meta -> meta.getPersistentDataContainer().set(visualStateKey, PersistentDataType.STRING, value));
    }

    private void mutate(ItemStack item, MetaMutation mutation) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        mutation.apply(meta);
        item.setItemMeta(meta);
    }

    @FunctionalInterface
    private interface MetaMutation {
        void apply(ItemMeta meta);
    }
}
