package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class FortuneCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Фортуна";
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Map<UUID, Integer> speedTasks = new HashMap<>();
    private final Map<UUID, Integer> luckParticleTasks = new HashMap<>();
    private final Random random = new Random();

    public FortuneCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyFortuneEffects(Player player) {
        if (!isFortune(player)) {
            removeFortuneEffects(player);
            return;
        }

        setFortuneSize(player);
        setFortuneHealth(player);
        applyPermanentLuck(player);
        startLuckParticles(player);
    }

    public void removeFortuneEffects(Player player) {
        UUID playerId = player.getUniqueId();

        restoreOriginalSize(player);
        restoreOriginalHealth(player);

        player.removePotionEffect(PotionEffectType.LUCK);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        stopSpeedEffect(player);
        stopLuckParticles(player);
        lastDamageTime.remove(playerId);
    }

    private void startLuckParticles(Player player) {
        UUID playerId = player.getUniqueId();
        stopLuckParticles(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isFortune(player)) {
                stopLuckParticles(player);
                return;
            }

            // Золотые искры удачи вокруг игрока
            player.getWorld().spawnParticle(Particle.GLOW,
                    player.getLocation().add(0, 1, 0), 3, 0.4, 0.6, 0.4, 0);
        }, 0L, 15L);

        luckParticleTasks.put(playerId, taskId);
    }

    private void stopLuckParticles(Player player) {
        UUID playerId = player.getUniqueId();
        if (luckParticleTasks.containsKey(playerId)) {
            Integer taskId = luckParticleTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            luckParticleTasks.remove(playerId);
        }
    }

    private void setFortuneSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Фортуны: " + e.getMessage());
        }
    }

    private void restoreOriginalSize(Player player) {
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось восстановить размер: " + e.getMessage());
        }
    }

    private void setFortuneHealth(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth(castName);
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                if (player.getHealth() > health) {
                    player.setHealth(health);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить здоровье Фортуны: " + e.getMessage());
        }
    }

    private void restoreOriginalHealth(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double healthPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * healthPercent);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось восстановить здоровье: " + e.getMessage());
        }
    }

    private void applyPermanentLuck(Player player) {
        boolean permanentLuck = plugin.getConfigManager().isFortunePermanentLuck();

        if (permanentLuck) {
            player.removePotionEffect(PotionEffectType.LUCK);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.LUCK,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false
            ));
        }
    }

    private void startSpeedEffect(Player player) {
        UUID playerId = player.getUniqueId();
        stopSpeedEffect(player);

        int speedLevel = plugin.getConfigManager().getFortuneSpeedLevel();
        int speedDuration = plugin.getConfigManager().getFortuneSpeedDuration();

        player.removePotionEffect(PotionEffectType.SPEED);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                speedDuration * 20,
                speedLevel,
                false,
                false
        ));

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (player.isOnline() && isFortune(player)) {
                player.removePotionEffect(PotionEffectType.SPEED);
            }
            speedTasks.remove(playerId);
        }, speedDuration * 20L);

        speedTasks.put(playerId, taskId);
        player.sendActionBar("§eУскорение! " + speedDuration + " секунд");
    }

    private void stopSpeedEffect(Player player) {
        UUID playerId = player.getUniqueId();
        if (speedTasks.containsKey(playerId)) {
            Integer taskId = speedTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            speedTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isFortune(player)) return;

        UUID playerId = player.getUniqueId();
        lastDamageTime.put(playerId, System.currentTimeMillis());

        double originalDamage = event.getDamage();
        double damageIncrease = plugin.getConfigManager().getFortuneDamageIncrease();
        event.setDamage(originalDamage * damageIncrease);

        startSpeedEffect(player);

        int totemChance = plugin.getConfigManager().getFortuneTotemChance();
        boolean blindnessOnTotem = plugin.getConfigManager().isFortuneBlindnessOnTotem();
        int blindnessDuration = plugin.getConfigManager().getFortuneBlindnessDuration();

        if (player.getHealth() - event.getFinalDamage() <= 0) {
            if (random.nextInt(100) < totemChance) {
                event.setCancelled(true);

                player.setHealth(1.0);
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.REGENERATION,
                        45 * 20,
                        1,
                        false,
                        false
                ));
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE,
                        40 * 20,
                        0,
                        false,
                        false
                ));

                if (blindnessOnTotem) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.BLINDNESS,
                            blindnessDuration,
                            0,
                            false,
                            true
                    ));
                    player.sendActionBar("§8Тьма накрывает вас после использования тотема!");
                }

                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        player.getLocation().add(0, 1, 0),
                        100, 0.5, 0.5, 0.5, 1.0);
                player.getWorld().playSound(player.getLocation(),
                        Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

                player.sendMessage("§6§lФортуна улыбнулась тебе! Ты выжил!");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isFortune(player)) return;

        if (!event.isCancelled()) {
            boolean blindnessOnTotem = plugin.getConfigManager().isFortuneBlindnessOnTotem();
            int blindnessDuration = plugin.getConfigManager().getFortuneBlindnessDuration();

            if (blindnessOnTotem) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.BLINDNESS,
                            blindnessDuration,
                            0,
                            false,
                            true
                    ));
                    player.sendActionBar("§8Тьма накрывает вас после использования тотема!");
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isFortune(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyFortuneEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isFortune(player)) {
            removeFortuneEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isFortune(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyFortuneEffects(player);
            }, 5L);
        }
    }

    private boolean isFortune(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}
