package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RusalCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Русал";
    private final Map<UUID, Boolean> isInWaterMap = new HashMap<>();
    private final Map<UUID, Integer> healthTasks = new HashMap<>();
    private final Map<UUID, Integer> dolphinGraceTasks = new HashMap<>();
    private final Map<UUID, Integer> hasteTasks = new HashMap<>();
    private final Map<UUID, ItemStack> tridentItems = new HashMap<>();
    private final Map<UUID, Integer> turtleHelmetDurabilityTask = new HashMap<>();
    private final Map<UUID, Integer> conduitCheckTasks = new HashMap<>();

    private static final String TRIDENT_NAME = "§bВолшебный трезубец Русала";

    public RusalCast(JustRaces plugin) {
        this.plugin = plugin;
        startAirTask();
        startHealthTask();
        startSwimTrailTask();
    }

    private final Map<UUID, Location> lastSwimLocation = new HashMap<>();

    private void startSwimTrailTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isRusal(player)) continue;
                if (!isInWater(player)) {
                    lastSwimLocation.remove(player.getUniqueId());
                    continue;
                }

                UUID playerId = player.getUniqueId();
                Location current = player.getLocation();
                Location last = lastSwimLocation.get(playerId);

                boolean moved = last == null
                        || last.getWorld() == null
                        || !last.getWorld().equals(current.getWorld())
                        || last.distanceSquared(current) > 0.04;

                // След из пузырей только если игрок действительно перемещается (плавает)
                if (moved) {
                    player.getWorld().spawnParticle(Particle.BUBBLE,
                            current.clone().add(0, 0.2, 0), 2, 0.15, 0.1, 0.15, 0.01);
                    lastSwimLocation.put(playerId, current.clone());
                }
            }
        }, 0L, 4L);
    }

    public void applyRusalEffects(Player player) {
        if (!isRusal(player)) {
            removeRusalEffects(player);
            return;
        }

        UUID playerId = player.getUniqueId();

        setRusalSize(player);
        setRusalHealth(player);

        startPlayerHealthTimer(player);
        startDolphinGraceTimer(player);
        startHasteTimer(player);
        startTurtleHelmetDurabilityTimer(player);
        startConduitCheckTimer(player);

        giveMagicTrident(player);
    }

    public void removeRusalEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopPlayerHealthTimer(player);
        stopDolphinGraceTimer(player);
        stopHasteTimer(player);
        stopTurtleHelmetDurabilityTimer(player);
        stopConduitCheckTimer(player);
        lastSwimLocation.remove(playerId);
        restoreOriginalHealth(player);
        restoreOriginalSize(player);

        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
        player.setRemainingAir(300);

        isInWaterMap.remove(playerId);
        removeMagicTrident(player);
        tridentItems.remove(playerId);
    }

    private void setRusalSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Русала: " + e.getMessage());
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

    private void setRusalHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Русала: " + e.getMessage());
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
            plugin.getLogger().warning("Ошибка при восстановлении здоровья: " + e.getMessage());
        }
    }

    private boolean isInRain(Player player) {
        World world = player.getWorld();
        if (!world.hasStorm()) return false;

        Location loc = player.getLocation();
        if (loc.getBlock().getLightFromSky() < 15) return false;

        for (int y = loc.getBlockY() + 1; y <= world.getMaxHeight(); y++) {
            Location checkLoc = new Location(world, loc.getX(), y, loc.getZ());
            if (!checkLoc.getBlock().isPassable()) {
                return false;
            }
        }
        return true;
    }

    private boolean canRusalBreathe(Player player) {
        return isInWater(player) || isInRain(player) || hasActiveConduitNearby(player);
    }

    private void startConduitCheckTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopConduitCheckTimer(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRusal(player)) {
                stopConduitCheckTimer(player);
                return;
            }

            boolean hasNearbyConduit = hasActiveConduitNearby(player);
            if (hasNearbyConduit) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.CONDUIT_POWER,
                        60,
                        0,
                        false,
                        false
                ));
                if (plugin.getConfigManager().isRusalNightVisionInWater()) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.NIGHT_VISION,
                            60,
                            0,
                            false,
                            false
                    ));
                }
            }
        }, 0L, 20L);

        conduitCheckTasks.put(playerId, taskId);
    }

    private void stopConduitCheckTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (conduitCheckTasks.containsKey(playerId)) {
            Integer taskId = conduitCheckTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            conduitCheckTasks.remove(playerId);
        }
    }

    private boolean hasActiveConduitNearby(Player player) {
        int conduitRange = plugin.getConfigManager().getRusalConduitBreathingRange();
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        for (int x = -conduitRange; x <= conduitRange; x++) {
            for (int y = -conduitRange; y <= conduitRange; y++) {
                for (int z = -conduitRange; z <= conduitRange; z++) {
                    Location checkLoc = playerLoc.clone().add(x, y, z);
                    if (playerLoc.distance(checkLoc) > conduitRange) continue;

                    Block block = world.getBlockAt(checkLoc);
                    if (block.getType() == Material.CONDUIT) {
                        if (isConduitActive(block)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isConduitActive(Block conduitBlock) {
        Location center = conduitBlock.getLocation();
        World world = center.getWorld();
        int waterCount = 0;

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location checkLoc = center.clone().add(x, y, z);
                    Block block = world.getBlockAt(checkLoc);
                    if (block.getType() == Material.WATER || block.getType() == Material.WATER_CAULDRON) {
                        waterCount++;
                    }
                }
            }
        }
        return waterCount >= 8;
    }

    private boolean hasTurtleHelmet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null && helmet.getType() == Material.TURTLE_HELMET;
    }

    private ItemStack getTurtleHelmet(Player player) {
        return player.getInventory().getHelmet();
    }

    private int getUnbreakingLevel(ItemStack item) {
        if (item == null || !item.containsEnchantment(Enchantment.UNBREAKING)) return 0;
        return item.getEnchantmentLevel(Enchantment.UNBREAKING);
    }

    private int getDurabilityLossInterval(int unbreakingLevel) {
        switch (unbreakingLevel) {
            case 1: return 20;
            case 2: return 25;
            case 3: return 30;
            default: return 15;
        }
    }

    private void damageTurtleHelmet(Player player) {
        ItemStack helmet = getTurtleHelmet(player);
        if (helmet == null) return;

        short current = helmet.getDurability();
        short max = helmet.getType().getMaxDurability();

        if (current + 1 < max) {
            helmet.setDurability((short) (current + 1));
        } else {
            player.getInventory().setHelmet(null);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage("§cВаш черепаший панцирь разрушился!");
        }
        player.updateInventory();
    }

    private void startTurtleHelmetDurabilityTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopTurtleHelmetDurabilityTimer(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRusal(player)) {
                stopTurtleHelmetDurabilityTimer(player);
                return;
            }

            if (!hasTurtleHelmet(player)) return;

            if (isInRain(player)) return;

            if (canRusalBreathe(player)) return;

            ItemStack helmet = getTurtleHelmet(player);
            if (helmet == null) return;

            int unbreaking = getUnbreakingLevel(helmet);
            int intervalSec = getDurabilityLossInterval(unbreaking);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && isRusal(player) && hasTurtleHelmet(player)) {
                    if (isInRain(player)) return;
                    if (!canRusalBreathe(player)) {
                        damageTurtleHelmet(player);
                    }
                }
            }, intervalSec * 20L);

        }, 0L, 100L);

        turtleHelmetDurabilityTask.put(playerId, taskId);
    }

    private void stopTurtleHelmetDurabilityTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (turtleHelmetDurabilityTask.containsKey(playerId)) {
            Integer taskId = turtleHelmetDurabilityTask.get(playerId);
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
            turtleHelmetDurabilityTask.remove(playerId);
        }
    }

    private void startAirTask() {
        int airDepletionRate = plugin.getConfigManager().getRusalAirDepletionRate();
        double drowningDamage = plugin.getConfigManager().getRusalDrowningDamage();
        boolean nightVisionInWater = plugin.getConfigManager().isRusalNightVisionInWater();

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isRusal(player)) continue;

                UUID playerId = player.getUniqueId();
                boolean inWater = isInWater(player);
                boolean inRain = isInRain(player);
                boolean wasInWater = isInWaterMap.getOrDefault(playerId, false);
                boolean canBreathe = canRusalBreathe(player);

                if (inWater != wasInWater) {
                    isInWaterMap.put(playerId, inWater);
                    player.setRemainingAir(300);

                    if (!inWater) {
                        // Капли при выходе из воды
                        player.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                                player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
                    } else {
                        // Всплеск при входе в воду
                        player.getWorld().spawnParticle(Particle.SPLASH,
                                player.getLocation(), 20, 0.4, 0.2, 0.4, 0.1);
                    }
                }

                if (inWater) {
                    // Пузыри в воде
                    player.getWorld().spawnParticle(Particle.BUBBLE,
                            player.getLocation().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.02);
                }

                if (canBreathe) {
                    player.setRemainingAir(300);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                    if (nightVisionInWater) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false));
                    }
                    continue;
                }

                boolean hasTurtle = hasTurtleHelmet(player);
                boolean hasWaterBreathing = player.hasPotionEffect(PotionEffectType.WATER_BREATHING);

                if (hasTurtle || hasWaterBreathing) {
                    player.setRemainingAir(300);
                    continue;
                }

                int airLoss = airDepletionRate * 4;

                int currentAir = player.getRemainingAir();
                if (currentAir > 0) {
                    player.setRemainingAir(currentAir - airLoss);
                }

                if (player.getRemainingAir() <= 0) {
                    player.damage(drowningDamage);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 1.0f);
                    player.setRemainingAir(0);
                }

                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, false, false));
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 0, false, false));
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }, 0L, 20L);
    }

    private void startHasteTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopHasteTimer(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRusal(player)) {
                stopHasteTimer(player);
                return;
            }
            if (isInWater(player)) {
                player.removePotionEffect(PotionEffectType.HASTE);
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 120, 2, false, false));
            } else {
                player.removePotionEffect(PotionEffectType.HASTE);
            }
        }, 0L, 100L);

        hasteTasks.put(playerId, taskId);
    }

    private void stopHasteTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (hasteTasks.containsKey(playerId)) {
            Integer taskId = hasteTasks.get(playerId);
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
            hasteTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.HASTE);
    }

    private void startDolphinGraceTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopDolphinGraceTimer(player);

        int dolphinGraceLevel = plugin.getConfigManager().getRusalDolphinGraceLevel();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRusal(player)) {
                stopDolphinGraceTimer(player);
                return;
            }
            if (isInWater(player)) {
                player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 120, dolphinGraceLevel, false, false));
            } else {
                player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            }
        }, 0L, 100L);

        dolphinGraceTasks.put(playerId, taskId);
    }

    private void stopDolphinGraceTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (dolphinGraceTasks.containsKey(playerId)) {
            Integer taskId = dolphinGraceTasks.get(playerId);
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
            dolphinGraceTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
    }

    private void startHealthTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isRusal(player)) {
                    updateRusalHealth(player, isInWater(player));
                }
            }
        }, 0L, 20L);
    }

    private void startPlayerHealthTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopPlayerHealthTimer(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRusal(player)) {
                stopPlayerHealthTimer(player);
                return;
            }
            updateRusalHealth(player, isInWater(player));
        }, 0L, 20L);

        healthTasks.put(playerId, taskId);
    }

    private void stopPlayerHealthTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (healthTasks.containsKey(playerId)) {
            Integer taskId = healthTasks.get(playerId);
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
            healthTasks.remove(playerId);
        }
    }

    private void updateRusalHealth(Player player, boolean inWater) {
        try {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth == null) return;

            if (inWater) {
                double target = 23.0;
                if (maxHealth.getBaseValue() != target) {
                    double percent = player.getHealth() / maxHealth.getBaseValue();
                    maxHealth.setBaseValue(target);
                    player.setHealth(Math.min(target, target * percent));
                }
            } else {
                double defaultHealth = 20.0;
                if (maxHealth.getBaseValue() != defaultHealth) {
                    double percent = player.getHealth() / maxHealth.getBaseValue();
                    maxHealth.setBaseValue(defaultHealth);
                    player.setHealth(defaultHealth * percent);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка обновления здоровья Русала: " + e.getMessage());
        }
    }

    private ItemStack createMagicTrident() {
        ItemStack trident = new ItemStack(Material.TRIDENT, 1);
        ItemMeta meta = trident.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TRIDENT_NAME);
            meta.setLore(List.of("§7Волшебный трезубец Русала", "§cНельзя выбросить или положить в сундук!"));
            meta.addEnchant(Enchantment.LOYALTY, 3, true);
            meta.addEnchant(Enchantment.IMPALING, 5, true);
            meta.addEnchant(Enchantment.CHANNELING, 1, true);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.setUnbreakable(true);
            trident.setItemMeta(meta);
        }
        return trident;
    }

    private boolean isMagicTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(TRIDENT_NAME);
    }

    private void giveMagicTrident(Player player) {
        UUID playerId = player.getUniqueId();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isMagicTrident(item)) {
                tridentItems.put(playerId, item);
                return;
            }
        }
        ItemStack trident = createMagicTrident();
        tridentItems.put(playerId, trident);
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
                player.getInventory().setItem(i, trident);
                break;
            }
        }
        player.updateInventory();
    }

    private void removeMagicTrident(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isMagicTrident(item)) player.getInventory().setItem(i, null);
        }
        player.updateInventory();
    }

    private boolean isRusal(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }

    private boolean isInWater(Player player) {
        return player.isInWater() ||
                player.getLocation().getBlock().getType() == Material.WATER ||
                player.getLocation().getBlock().getType() == Material.BUBBLE_COLUMN ||
                player.getEyeLocation().getBlock().getType() == Material.WATER;
    }

    private boolean isRawFish(Material mat) {
        return mat == Material.COD || mat == Material.SALMON ||
                mat == Material.TROPICAL_FISH || mat == Material.PUFFERFISH;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isRusal(player)) return;
        if (isMagicTrident(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isRusal(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyRusalEffects(player), 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isRusal(player)) removeRusalEffects(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isRusal(player)) {
            isInWaterMap.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyRusalEffects(player), 5L);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isRusal(player)) return;
        if (isInWater(player)) {
            player.getWorld().spawnParticle(Particle.BUBBLE, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!isRusal(player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING && isInWater(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!isRusal(player)) return;
        Material type = event.getItem().getType();

        if (type == Material.POTION || type == Material.WATER_BUCKET) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.setRemainingAir(300), 1L);
        }

        if (isRawFish(type)) {
            int fishFoodRestore = plugin.getConfigManager().getRusalFishFoodRestore();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + fishFoodRestore));
                player.setSaturation(Math.min(20, player.getSaturation() + (fishFoodRestore * 1.2f)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.0f);
            }, 1L);
            return;
        }

        if (type.isEdible() && !isRawFish(type)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0, false, false));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.3f, 0.8f);
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isRusal(player)) return;
        event.getDrops().removeIf(this::isMagicTrident);
    }
}
