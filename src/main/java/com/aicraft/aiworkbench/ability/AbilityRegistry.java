package com.aicraft.aiworkbench.ability;

import com.aicraft.aiworkbench.ai.model.AbilityDefinition;
import com.aicraft.aiworkbench.ai.model.AbilityType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityRegistry {
    private final JavaPlugin plugin;
    private final Map<AbilityType, AbilityExecutor> executors = new EnumMap<>(AbilityType.class);
    private final Map<UUID, Map<AbilityType, Long>> cooldowns = new ConcurrentHashMap<>();

    public AbilityRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    public void execute(Player player, LivingEntity target, AbilityDefinition definition) {
        if (definition == null || onCooldown(player, definition)) {
            return;
        }
        AbilityExecutor executor = executors.get(definition.type());
        if (executor != null) {
            executor.execute(player, target, definition);
            cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(AbilityType.class))
                    .put(definition.type(), System.currentTimeMillis() + definition.cooldown() * 1000L);
        }
    }

    private boolean onCooldown(Player player, AbilityDefinition definition) {
        Long until = cooldowns.getOrDefault(player.getUniqueId(), Map.of()).get(definition.type());
        return until != null && until > System.currentTimeMillis();
    }

    private void registerDefaults() {
        executors.put(AbilityType.DAMAGE, (player, target, ability) -> target.damage(ability.power(), player));
        executors.put(AbilityType.FREEZE, (player, target, ability) -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ability.duration() * 20, 4)));
        executors.put(AbilityType.BURN, (player, target, ability) -> target.setFireTicks(ability.duration() * 20));
        executors.put(AbilityType.LIGHTNING, (player, target, ability) -> target.getWorld().strikeLightningEffect(target.getLocation()));
        executors.put(AbilityType.KNOCKBACK, (player, target, ability) -> target.setVelocity(target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(ability.power())));
        executors.put(AbilityType.POISON, (player, target, ability) -> target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ability.duration() * 20, 0)));
        executors.put(AbilityType.WITHER, (player, target, ability) -> target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, ability.duration() * 20, 0)));
        executors.put(AbilityType.LIFESTEAL, (player, target, ability) -> {
            target.damage(ability.power(), player);
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + ability.power()));
        });
        executors.put(AbilityType.AREA_DAMAGE, (player, target, ability) -> target.getNearbyEntities(ability.radius(), ability.radius(), ability.radius()).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .forEach(entity -> entity.damage(ability.power(), player)));
        executors.put(AbilityType.EXPLOSION, (player, target, ability) -> target.getWorld().createExplosion(target.getLocation(), (float) Math.min(2.0, ability.power()), false, false, player));
        executors.put(AbilityType.HEAL, (player, target, ability) -> player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + ability.power())));
        executors.put(AbilityType.SPEED, (player, target, ability) -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ability.duration() * 20, 0)));
        executors.put(AbilityType.STRENGTH, (player, target, ability) -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ability.duration() * 20, 0)));
        executors.put(AbilityType.DASH, (player, target, ability) -> player.setVelocity(player.getLocation().getDirection().multiply(Math.max(1.0, ability.power()))));
        executors.put(AbilityType.TELEPORT, (player, target, ability) -> player.teleport(player.getLocation().add(player.getLocation().getDirection().multiply(Math.min(8.0, ability.power() * 2)))));
        executors.put(AbilityType.PARTICLE_AURA, (player, target, ability) -> player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 40, 1, 1, 1));
        executors.put(AbilityType.RANDOM_EFFECT, (player, target, ability) -> player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1));
        executors.put(AbilityType.SHIELD, (player, target, ability) -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ability.duration() * 20, 0)));
        executors.put(AbilityType.PROJECTILE, (player, target, ability) -> player.launchProjectile(org.bukkit.entity.Snowball.class, player.getLocation().getDirection().multiply(ability.power())));
        executors.put(AbilityType.SUMMON, (player, target, ability) -> target.getWorld().spawn(target.getLocation(), org.bukkit.entity.Wolf.class));
    }
}
