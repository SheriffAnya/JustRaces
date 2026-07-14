package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class WardenCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Варден";
    private final Map<UUID, Integer> sculkDetectionTasks = new HashMap<>();
    private final Map<UUID, Integer> playerDetectionTasks = new HashMap<>();
    private final Map<UUID, Integer> echoShardTasks = new HashMap<>();
    private final Map<UUID, Long> echoShardCooldown = new HashMap<>();
    private final Map<UUID, Integer> ambientParticleTasks = new HashMap<>();
    private final Random random = new Random();
    private static final String ECHO_SHARD_NAME = "§3Осколок эха Вардена";
    private static final long ECHO_SHARD_COOLDOWN = 5000L;

    public WardenCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyWardenEffects(Player player) {
        if (!isWarden(player)) {
            removeWardenEffects(player);
            return;
        }

        setWardenSize(player);
        setWardenHealth(player);
        applyPermanentEffects(player);
        startSculkDetection(player);
        startPlayerDetection(player);
        startEchoShardTimer(player);
        startAmbientParticles(player);

        plugin.getArmorManager().removeForbiddenItems(player);
    }

    public void removeWardenEffects(Player player) {
        UUID playerId = player.getUniqueId();

        restoreOriginalSize(player);
        restoreOriginalHealth(player);
        stopSculkDetection(player);
        stopPlayerDetection(player);
        stopEchoShardTimer(player);
        stopAmbientParticles(player);
        removeAllEchoShards(player);

        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    private void startAmbientParticles(Player player) {
        UUID playerId = player.getUniqueId();
        stopAmbientParticles(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isWarden(player)) {
                stopAmbientParticles(player);
                return;
            }

            // Постоянное небольшое свечение вокруг игрока-Вардена
            player.getWorld().spawnParticle(Particle.SCULK_SOUL,
                    player.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.01);

            // Частицы при ходьбе по скалку
            Material blockBelow = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
            if (blockBelow == Material.SCULK || blockBelow == Material.SCULK_VEIN ||
                    blockBelow == Material.SCULK_CATALYST || blockBelow == Material.SCULK_SENSOR ||
                    blockBelow == Material.SCULK_SHRIEKER) {
                player.getWorld().spawnParticle(Particle.SCULK_CHARGE,
                        player.getLocation().add(0, 0.1, 0), 5, 0.2, 0.05, 0.2, 0.0f);
            }
        }, 0L, 10L);

        ambientParticleTasks.put(playerId, taskId);
    }

    private void stopAmbientParticles(Player player) {
        UUID playerId = player.getUniqueId();
        if (ambientParticleTasks.containsKey(playerId)) {
            Integer taskId = ambientParticleTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            ambientParticleTasks.remove(playerId);
        }
    }

    private void setWardenSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Вардена: " + e.getMessage());
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

    private void setWardenHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Вардена: " + e.getMessage());
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

    private void applyPermanentEffects(Player player) {
        int resistanceLevel = plugin.getConfigManager().getWardenPermanentResistance();

        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                Integer.MAX_VALUE,
                resistanceLevel,
                false,
                false
        ));
    }

    private void startEchoShardTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopEchoShardTimer(player);

        int interval = 30 * 20;

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isWarden(player)) {
                stopEchoShardTimer(player);
                return;
            }

            giveEchoShard(player);

        }, interval, interval);

        echoShardTasks.put(playerId, taskId);
    }

    private void stopEchoShardTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (echoShardTasks.containsKey(playerId)) {
            Integer taskId = echoShardTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            echoShardTasks.remove(playerId);
        }
    }

    private ItemStack createEchoShard() {
        ItemStack shard = new ItemStack(Material.ECHO_SHARD, 1);
        ItemMeta meta = shard.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ECHO_SHARD_NAME);

            List<String> lore = new ArrayList<>();
            int visionRange = plugin.getConfigManager().getWardenPlayerVisionRange();
            lore.add("§7Используйте, чтобы увидеть");
            lore.add("§7всех существ в радиусе " + visionRange + " блоков");
            meta.setLore(lore);

            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

            shard.setItemMeta(meta);
        }

        return shard;
    }

    private boolean isEchoShard(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() &&
                meta.getDisplayName().equals(ECHO_SHARD_NAME);
    }

    private void giveEchoShard(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isEchoShard(item)) {
                return;
            }
        }

        ItemStack shard = createEchoShard();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(shard);

        if (!leftover.isEmpty()) {
            World world = player.getWorld();
            Location dropLoc = player.getLocation().add(0, 1, 0);
            world.dropItem(dropLoc, shard);
        }

        player.sendActionBar("§3Вы получили Осколок эха!");
    }

    private void removeAllEchoShards(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isEchoShard(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isWarden(player)) return;

        ItemStack item = event.getItem();
        if (item == null || !isEchoShard(item)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (echoShardCooldown.containsKey(playerId)) {
            long remaining = echoShardCooldown.get(playerId) - currentTime;
            if (remaining > 0) {
                player.sendActionBar("§cОсколок эха перезаряжается: " + (remaining / 1000 + 1) + "с");
                return;
            }
        }

        echoShardCooldown.put(playerId, currentTime + ECHO_SHARD_COOLDOWN);

        if (event.getHand() == EquipmentSlot.HAND) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }
        player.updateInventory();

        int visionRange = plugin.getConfigManager().getWardenPlayerVisionRange();
        for (Entity entity : player.getNearbyEntities(visionRange, visionRange, visionRange)) {
            // Подсвечиваем ВСЕХ живых существ в радиусе, включая других игроков
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                entity.setGlowing(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (entity.isValid()) {
                        entity.setGlowing(false);
                    }
                }, 5 * 20L);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.0f);
        // Мощная вспышка осколка эха: радиус 5, 100 частиц
        player.getWorld().spawnParticle(Particle.SCULK_SOUL, player.getLocation().add(0, 1, 0), 100, 5, 5, 5, 0.15);
        player.sendActionBar("§3Эхо раскрывает всех существ рядом!");
    }

    private void startSculkDetection(Player player) {
        UUID playerId = player.getUniqueId();
        stopSculkDetection(player);

        int sculkRange = plugin.getConfigManager().getWardenSculkBonusRange();
        double damageBonus = plugin.getConfigManager().getWardenDamageBonus();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isWarden(player)) {
                stopSculkDetection(player);
                return;
            }

            boolean hasSculkNearby = hasSculkNearby(player, sculkRange);

            if (hasSculkNearby) {
                boolean alreadyActive = player.hasPotionEffect(PotionEffectType.STRENGTH);
                player.removePotionEffect(PotionEffectType.STRENGTH);
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        40,
                        (int) damageBonus,
                        false,
                        false
                ));
                if (!alreadyActive) {
                    // Частицы активации силы от Sculk
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            player.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.02);
                }
            } else {
                player.removePotionEffect(PotionEffectType.STRENGTH);
            }

        }, 0L, 20L);

        sculkDetectionTasks.put(playerId, taskId);
    }

    private void stopSculkDetection(Player player) {
        UUID playerId = player.getUniqueId();
        if (sculkDetectionTasks.containsKey(playerId)) {
            Integer taskId = sculkDetectionTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            sculkDetectionTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    private void startPlayerDetection(Player player) {
        UUID playerId = player.getUniqueId();
        stopPlayerDetection(player);

        int visionRange = plugin.getConfigManager().getWardenPlayerVisionRange();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isWarden(player)) {
                stopPlayerDetection(player);
                return;
            }

            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                if (nearbyPlayer.equals(player)) continue;

                if (player.getLocation().distance(nearbyPlayer.getLocation()) <= visionRange) {
                    player.getWorld().spawnParticle(Particle.GLOW,
                            nearbyPlayer.getLocation().add(0, 1, 0),
                            1, 0.3, 0.5, 0.3, 0);
                }
            }

        }, 0L, 40L);

        playerDetectionTasks.put(playerId, taskId);
    }

    private void stopPlayerDetection(Player player) {
        UUID playerId = player.getUniqueId();
        if (playerDetectionTasks.containsKey(playerId)) {
            Integer taskId = playerDetectionTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            playerDetectionTasks.remove(playerId);
        }
    }

    private boolean hasSculkNearby(Player player, int radius) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location checkLoc = playerLoc.clone().add(x, y, z);
                    Block block = world.getBlockAt(checkLoc);

                    if (block.getType() == Material.SCULK ||
                            block.getType() == Material.SCULK_SENSOR ||
                            block.getType() == Material.SCULK_CATALYST ||
                            block.getType() == Material.SCULK_SHRIEKER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!isWarden(player)) return;

            int stunChance = plugin.getConfigManager().getWardenStunChance();
            int stunDuration = plugin.getConfigManager().getWardenStunDuration();

            if (random.nextInt(100) < stunChance && event.getDamager() instanceof LivingEntity) {
                LivingEntity damager = (LivingEntity) event.getDamager();

                damager.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        stunDuration,
                        127,
                        false,
                        false
                ));
                damager.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        stunDuration,
                        0,
                        false,
                        false
                ));
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() == null) return;
        if (!(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)) return;

        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
        if (!(damageEvent.getDamager() instanceof Player)) return;

        Player player = (Player) damageEvent.getDamager();
        if (!isWarden(player)) return;

        boolean spawnSculkOnKill = plugin.getConfigManager().isWardenSpawnSculkOnKill();

        if (spawnSculkOnKill && !(event.getEntity() instanceof Player)) {
            Location deathLoc = event.getEntity().getLocation();
            World world = deathLoc.getWorld();

            for (int i = 0; i < 3; i++) {
                int offsetX = random.nextInt(3) - 1;
                int offsetZ = random.nextInt(3) - 1;

                Location sculkLoc = deathLoc.clone().add(offsetX, 0, offsetZ);
                Block block = world.getBlockAt(sculkLoc);

                if (block.getType() == Material.AIR || block.getType() == Material.GRASS_BLOCK || block.getType() == Material.SHORT_GRASS) {
                    block.setType(Material.SCULK);
                }
            }

            world.playSound(deathLoc, Sound.BLOCK_SCULK_SPREAD, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isWarden(player)) return;

        boolean leaveSculkOnDeath = plugin.getConfigManager().isWardenLeaveSculkOnDeath();

        if (leaveSculkOnDeath) {
            Location deathLoc = player.getLocation();
            deathLoc.getBlock().setType(Material.SCULK_CATALYST);
        }

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isEchoShard(item)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isWarden(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyWardenEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isWarden(player)) {
            removeWardenEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isWarden(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyWardenEffects(player);
            }, 5L);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;

        Player player = (Player) event.getTarget();
        if (!isWarden(player)) return;

        boolean ignoreByWardens = plugin.getConfigManager().isWardenIgnoreByWardens();

        if (ignoreByWardens && (event.getEntity() instanceof Warden ||
                event.getEntityType().toString().contains("SCULK"))) {
            event.setCancelled(true);
        }
    }

    private boolean isWarden(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}
