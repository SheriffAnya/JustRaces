package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class EnderCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Эндермен";

    private final Map<UUID, Integer> pearlTasks = new HashMap<>();
    private final Map<UUID, Integer> waterDamageTasks = new HashMap<>();
    private final Map<UUID, Integer> rainDamageTasks = new HashMap<>();
    private final Map<UUID, Integer> ambientParticleTasks = new HashMap<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private final Map<UUID, Long> pearlDamageImmunity = new HashMap<>();
    private final Set<UUID> enderPearls = new HashSet<>();

    public EnderCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyEnderEffects(Player player) {
        if (!isEnder(player)) {
            removeEnderEffects(player);
            return;
        }

        setEnderSize(player);
        setEnderHealth(player);
        setAttackRange(player);
        startPearlTimer(player);
        startWaterDamageTimer(player);
        startRainDamageTimer(player);
        startAmbientParticles(player);
    }

    public void removeEnderEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopPearlTimer(player);
        stopWaterDamageTimer(player);
        stopRainDamageTimer(player);
        stopAmbientParticles(player);
        restoreOriginalSize(player);
        restoreOriginalHealth(player);
        restoreOriginalAttackRange(player);

        lastAttackTime.remove(playerId);
        pearlDamageImmunity.remove(playerId);
    }

    private void startAmbientParticles(Player player) {
        UUID playerId = player.getUniqueId();
        stopAmbientParticles(player);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isEnder(player)) {
                stopAmbientParticles(player);
                return;
            }

            // Постоянные портальные частицы вокруг игрока-Эндермена
            player.getWorld().spawnParticle(Particle.PORTAL,
                    player.getLocation().add(0, 1, 0), 4, 0.3, 0.6, 0.3, 0.3);
        }, 0L, 10L);

        ambientParticleTasks.put(playerId, taskId);
    }

    private void stopAmbientParticles(Player player) {
        UUID playerId = player.getUniqueId();
        if (ambientParticleTasks.containsKey(playerId)) {
            Integer taskId = ambientParticleTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            ambientParticleTasks.remove(playerId);
        }
    }

    private void setEnderSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Эндермена: " + e.getMessage());
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

    private void setEnderHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Эндермена: " + e.getMessage());
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

    private void setAttackRange(Player player) {
        try {
            double attackRange = plugin.getConfigManager().getEnderAttackRange();
            AttributeInstance attackRangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
            if (attackRangeAttr != null) {
                attackRangeAttr.setBaseValue(attackRange);
            }

            AttributeInstance blockRangeAttr = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
            if (blockRangeAttr != null) {
                blockRangeAttr.setBaseValue(attackRange);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить дальность атаки: " + e.getMessage());
        }
    }

    private void restoreOriginalAttackRange(Player player) {
        try {
            AttributeInstance attackRangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
            if (attackRangeAttr != null) {
                attackRangeAttr.setBaseValue(3.0);
            }

            AttributeInstance blockRangeAttr = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
            if (blockRangeAttr != null) {
                blockRangeAttr.setBaseValue(4.5);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось восстановить дальность атаки: " + e.getMessage());
        }
    }

    private void startPearlTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopPearlTimer(player);

        int interval = plugin.getConfigManager().getEnderPearlInterval() * 20;

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isEnder(player)) {
                stopPearlTimer(player);
                return;
            }

            giveEnderPearl(player);
        }, 0L, interval);

        pearlTasks.put(playerId, taskId);
    }

    private void stopPearlTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (pearlTasks.containsKey(playerId)) {
            Integer taskId = pearlTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            pearlTasks.remove(playerId);
        }
    }

    private void startWaterDamageTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopWaterDamageTimer(player);

        double waterDamage = plugin.getConfigManager().getEnderWaterDamage();

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isEnder(player)) {
                stopWaterDamageTimer(player);
                return;
            }

            if (isInWater(player)) {
                player.damage(waterDamage);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.7f, 0.8f);
            }
        }, 0L, 20L);

        waterDamageTasks.put(playerId, taskId);
    }

    private void stopWaterDamageTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (waterDamageTasks.containsKey(playerId)) {
            Integer taskId = waterDamageTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            waterDamageTasks.remove(playerId);
        }
    }

    private void startRainDamageTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopRainDamageTimer(player);

        double rainDamage = plugin.getConfigManager().getEnderRainDamage();

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isEnder(player)) {
                stopRainDamageTimer(player);
                return;
            }

            if (isInRain(player)) {
                player.damage(rainDamage);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.5f, 1.0f);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }, 0L, 20L);

        rainDamageTasks.put(playerId, taskId);
    }

    private void stopRainDamageTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (rainDamageTasks.containsKey(playerId)) {
            Integer taskId = rainDamageTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            rainDamageTasks.remove(playerId);
        }
    }

    private void giveEnderPearl(Player player) {
        int maxPearls = plugin.getConfigManager().getEnderMaxPearls();
        int pearlCount = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                pearlCount += item.getAmount();
            }
        }

        if (pearlCount >= maxPearls) {
            return;
        }

        ItemStack pearl = new ItemStack(Material.ENDER_PEARL, 1);
        ItemMeta meta = pearl.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5Жемчуг Эндермена");
            meta.setLore(Arrays.asList("§7Выпадает каждые " + plugin.getConfigManager().getEnderPearlInterval() + " секунд"));
            pearl.setItemMeta(meta);
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(pearl);

        if (!leftover.isEmpty()) {
            World world = player.getWorld();
            Location dropLoc = player.getLocation().add(0, 1, 0);
            world.dropItem(dropLoc, pearl);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.2f);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isEnder(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyEnderEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isEnder(player)) {
            removeEnderEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isEnder(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyEnderEffects(player);
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (!isEnder(player)) return;

            UUID playerId = player.getUniqueId();
            if (pearlDamageImmunity.containsKey(playerId)) {
                long immunityEnd = pearlDamageImmunity.get(playerId);
                if (System.currentTimeMillis() < immunityEnd) {
                    event.setCancelled(true);
                    return;
                } else {
                    pearlDamageImmunity.remove(playerId);
                }
            }

            double attackRange = plugin.getConfigManager().getEnderAttackRange();
            if (event.getEntity().getLocation().distance(player.getLocation()) > attackRange) {
                event.setCancelled(true);
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (lastAttackTime.containsKey(playerId)) {
                if (currentTime - lastAttackTime.get(playerId) < 500) {
                    event.setCancelled(true);
                    return;
                }
            }
            lastAttackTime.put(playerId, currentTime);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 1.0f);
        }

        if (event.getEntity() instanceof Player && event.getDamager() instanceof Enderman) {
            Player player = (Player) event.getEntity();
            if (isEnder(player)) {
                event.setCancelled(true);

                Enderman enderman = (Enderman) event.getDamager();
                Location playerLoc = player.getLocation();
                Vector direction = playerLoc.toVector().subtract(enderman.getLocation().toVector()).normalize();
                enderman.setVelocity(direction.multiply(-1.5).setY(0.5));
            }
        }

        if (event.getEntity() instanceof Player && event.getDamager() instanceof Endermite) {
            Player player = (Player) event.getEntity();
            if (isEnder(player)) {
                event.setCancelled(true);
            }
        }

        if (event.getDamager() instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) event.getDamager();

            if (enderPearls.contains(pearl.getUniqueId())) {
                event.setCancelled(true);

                if (pearl.getShooter() instanceof Player) {
                    Player shooter = (Player) pearl.getShooter();
                    if (isEnder(shooter)) {
                        pearlDamageImmunity.put(shooter.getUniqueId(), System.currentTimeMillis() + 3000);

                        int spawnChance = plugin.getConfigManager().getEnderEndermiteSpawnChance();
                        if (new Random().nextInt(100) < spawnChance) {
                            Location spawnLoc = pearl.getLocation();
                            Endermite endermite = spawnLoc.getWorld().spawn(spawnLoc, Endermite.class);
                            endermite.setTarget(null);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        if (!isEnder(player)) return;

        enderPearls.add(event.getEntity().getUniqueId());
        pearlDamageImmunity.put(player.getUniqueId(), System.currentTimeMillis() + 3000);

        final UUID pearlId = event.getEntity().getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            enderPearls.remove(pearlId);
        }, 20L * 30L);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!isEnder(player)) return;

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            // Вспышка частиц на месте телепортации (перл Эндермена)
            Location to = event.getTo();
            if (to != null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.getWorld().spawnParticle(Particle.PORTAL,
                            to.clone().add(0, 1, 0), 40, 0.4, 0.7, 0.4, 0.4);
                });
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;

        Player player = (Player) event.getTarget();
        if (!isEnder(player)) return;

        if (event.getEntity() instanceof Enderman) {
            event.setCancelled(true);
        }

        if (event.getEntity() instanceof Endermite) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isEnder(player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            if (isInWater(player)) {
                double waterDamage = plugin.getConfigManager().getEnderWaterDamage();
                event.setDamage(waterDamage);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.7f, 0.8f);
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
            if (isInRain(player)) {
                double rainDamage = plugin.getConfigManager().getEnderRainDamage();
                event.setDamage(rainDamage);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.5f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!isEnder(player)) return;

        ItemStack item = event.getItem();
        boolean chorusBonus = plugin.getConfigManager().isEnderChorusFruitBonus();

        if (!isChorusFruit(item)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.getFoodLevel() > 5) {
                    player.setFoodLevel(Math.max(5, player.getFoodLevel() - 5));
                }
                if (player.getSaturation() > 1) {
                    player.setSaturation(Math.max(1, player.getSaturation() - 3));
                }
                player.sendActionBar("§cОбычная еда плохо насыщает Эндермена!");
            }, 1L);
        } else if (chorusBonus) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.getFoodLevel() < 20) {
                    player.setFoodLevel(Math.min(20, player.getFoodLevel() + 4));
                }
                if (player.getSaturation() < 20) {
                    player.setSaturation(Math.min(20, player.getSaturation() + 2.4f));
                }
                player.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.5f, 1.0f);
                player.sendActionBar("§aХорус хорошо насыщает вас!");
            }, 1L);
        }
    }

    private boolean isEnder(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }

    private boolean isInWater(Player player) {
        Location loc = player.getLocation();
        Material blockAtFeet = loc.getBlock().getType();
        Material blockAtEyes = player.getEyeLocation().getBlock().getType();

        return blockAtFeet == Material.WATER ||
                blockAtFeet == Material.BUBBLE_COLUMN ||
                blockAtEyes == Material.WATER ||
                blockAtEyes == Material.BUBBLE_COLUMN ||
                player.isInWater();
    }

    private boolean isInRain(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        if (!world.hasStorm()) return false;
        if (loc.getBlock().getLightFromSky() < 15) return false;

        for (int y = loc.getBlockY() + 1; y <= world.getMaxHeight(); y++) {
            Location checkLoc = new Location(world, loc.getX(), y, loc.getZ());
            if (!checkLoc.getBlock().isPassable()) {
                return false;
            }
        }

        return true;
    }

    private boolean isChorusFruit(ItemStack item) {
        return item != null && item.getType() == Material.CHORUS_FRUIT;
    }
}
