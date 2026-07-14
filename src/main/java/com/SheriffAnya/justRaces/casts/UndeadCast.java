package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.*;

public class UndeadCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Нежить";

    private final Map<UUID, ItemStack> specialBows = new HashMap<>();
    private final Map<UUID, Integer> sunDamageTasks = new HashMap<>();
    private final Map<UUID, Integer> darknessTasks = new HashMap<>();
    private final Map<UUID, Integer> helmetDurabilityTasks = new HashMap<>();

    public UndeadCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyUndeadEffects(Player player) {
        if (!isUndead(player)) {
            removeUndeadEffects(player);
            return;
        }

        setUndeadSize(player);
        setUndeadHealth(player);
        giveSpecialBowIfNeeded(player);
        startSunDamageTimer(player);
        startDarknessTimer(player);
        startHelmetDurabilityTimer(player);
    }

    public void removeUndeadEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopSunDamageTimer(player);
        stopDarknessTimer(player);
        stopHelmetDurabilityTimer(player);
        restoreOriginalSize(player);
        restoreOriginalHealth(player);

        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.setFireTicks(0);

        // Удаляем специальный лук из инвентаря
        removeSpecialBow(player);
        specialBows.remove(playerId);
    }

    private void removeSpecialBow(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isSpecialBow(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && isSpecialBow(offHand)) {
            player.getInventory().setItemInOffHand(null);
        }
        player.updateInventory();
    }

    // ===== Остальные методы без изменений (скопированы из исходного кода) =====

    private void setUndeadSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Нежити: " + e.getMessage());
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

    private void setUndeadHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Нежити: " + e.getMessage());
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

    private void giveSpecialBowIfNeeded(Player player) {
        UUID playerId = player.getUniqueId();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BOW && isSpecialBow(item)) {
                specialBows.put(playerId, item);
                return;
            }
        }

        ItemStack specialBow = createSpecialBow();
        specialBows.put(playerId, specialBow);

        for (int i = 0; i < 9; i++) {
            ItemStack slotItem = player.getInventory().getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                player.getInventory().setItem(i, specialBow);
                break;
            }
        }

        player.updateInventory();
    }

    private ItemStack createSpecialBow() {
        ItemStack bow = new ItemStack(Material.BOW, 1);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§4Лук Нежити");
            meta.setLore(Arrays.asList("§7Особый лук Нежити", "§cНельзя выбросить или положить в сундук!"));

            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addEnchant(Enchantment.FLAME, 1, true);
            meta.addEnchant(Enchantment.POWER, 2, true);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.setUnbreakable(true);

            bow.setItemMeta(meta);
        }

        return bow;
    }

    private boolean isSpecialBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().equals("§4Лук Нежити");
    }

    private boolean isWearingHelmet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() == Material.AIR) return false;
        // Только настоящие шлемы защищают Нежить от солнца.
        // Черепа, головы игроков/мобов и тыквы — НЕ считаются.
        String typeName = helmet.getType().name();
        return typeName.endsWith("_HELMET");
    }

    private ItemStack getHelmet(Player player) {
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

    private void damageHelmet(Player player) {
        ItemStack helmet = getHelmet(player);
        if (helmet == null) return;

        if (helmet.getType().getMaxDurability() <= 0) return;

        short currentDurability = helmet.getDurability();
        short maxDurability = helmet.getType().getMaxDurability();

        if (currentDurability + 1 < maxDurability) {
            helmet.setDurability((short) (currentDurability + 1));
        } else {
            player.getInventory().setHelmet(null);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage("§cВаш шлем разрушился от солнечного света!");
        }

        player.updateInventory();
    }

    private void startHelmetDurabilityTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopHelmetDurabilityTimer(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isUndead(player)) {
                stopHelmetDurabilityTimer(player);
                return;
            }

            if (!isWearingHelmet(player)) return;

            if (isRainingAtPlayer(player)) return;

            if (!isExposedToSun(player)) return;

            ItemStack helmet = getHelmet(player);
            if (helmet == null) return;

            int unbreakingLevel = getUnbreakingLevel(helmet);
            int interval = getDurabilityLossInterval(unbreakingLevel);

            if (helmetDurabilityTasks.containsKey(playerId)) {
                Integer oldTask = helmetDurabilityTasks.get(playerId);
                if (oldTask != null) {
                    Bukkit.getScheduler().cancelTask(oldTask);
                }
            }

            int damageTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (!player.isOnline() || !isUndead(player) || !isWearingHelmet(player)) return;
                if (isRainingAtPlayer(player)) return;
                if (!isExposedToSun(player)) return;
                damageHelmet(player);
            }, interval * 20L, interval * 20L);

            helmetDurabilityTasks.put(playerId, damageTaskId);

        }, 0L, 100L);

        helmetDurabilityTasks.put(playerId, taskId);
    }

    private void stopHelmetDurabilityTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (helmetDurabilityTasks.containsKey(playerId)) {
            Integer taskId = helmetDurabilityTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            helmetDurabilityTasks.remove(playerId);
        }
    }

    private void startSunDamageTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopSunDamageTimer(player);

        double sunDamage = plugin.getConfigManager().getUndeadSunDamage();

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isUndead(player)) {
                stopSunDamageTimer(player);
                return;
            }

            if (isWearingHelmet(player)) {
                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
                return;
            }

            boolean exposedToSun = isExposedToSun(player);

            if (exposedToSun) {
                player.damage(sunDamage);

                if (!isRainingAtPlayer(player)) {
                    player.setFireTicks(40);
                } else {
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
                }

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 0.5f, 1.0f);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.01);
            } else {
                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
            }

        }, 0L, 20L);

        sunDamageTasks.put(playerId, taskId);
    }

    private void stopSunDamageTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (sunDamageTasks.containsKey(playerId)) {
            Integer taskId = sunDamageTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            sunDamageTasks.remove(playerId);
        }
    }

    private boolean isRainingAtPlayer(Player player) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return false;
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

    private void startDarknessTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopDarknessTimer(player);

        boolean blindnessInLight = plugin.getConfigManager().isUndeadBlindnessInLight();
        boolean nightVisionInDarkness = plugin.getConfigManager().isUndeadNightVisionInDarkness();

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isUndead(player)) {
                stopDarknessTimer(player);
                return;
            }

            boolean inDarkness = isInDarkness(player);

            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.DARKNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.HUNGER);

            if (inDarkness && nightVisionInDarkness) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false));
                player.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                        player.getLocation().add(0, 1, 0), 4, 0.4, 0.6, 0.4, 0);
            } else if (!inDarkness && blindnessInLight) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0, false, false));
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, false, false));

        }, 0L, 100L);

        darknessTasks.put(playerId, taskId);
    }

    private void stopDarknessTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (darknessTasks.containsKey(playerId)) {
            Integer taskId = darknessTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            darknessTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }

    private boolean isExposedToSun(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        if (world.getEnvironment() != World.Environment.NORMAL) return false;

        long time = world.getTime();
        boolean isDay = time > 0 && time < 13000;

        if (!isDay) return false;
        if (loc.getBlock().getLightFromSky() < 15) return false;

        for (int y = loc.getBlockY() + 1; y <= world.getMaxHeight(); y++) {
            Location checkLoc = new Location(world, loc.getX(), y, loc.getZ());
            if (!checkLoc.getBlock().isPassable()) {
                return false;
            }
        }

        return true;
    }

    private boolean isInDarkness(Player player) {
        Location loc = player.getLocation();
        int lightLevel = loc.getBlock().getLightLevel();
        return lightLevel < 8;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isUndead(player)) return;

        DamageCause cause = event.getCause();

        if (cause == DamageCause.FIRE_TICK || cause == DamageCause.FIRE || cause == DamageCause.LAVA) {
            if (isRainingAtPlayer(player)) {
                event.setCancelled(true);
                player.setFireTicks(0);

                if (player.getFireTicks() > 0) {
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 5, 0.2, 0.5, 0.2, 0.01);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                }
                return;
            }

            if (isWearingHelmet(player) && isExposedToSun(player)) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }

            if (cause == DamageCause.FIRE_TICK && isExposedToSun(player)) {
                event.setDamage(0);
                double sunDamage = plugin.getConfigManager().getUndeadSunDamage();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.damage(sunDamage);
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!isUndead(player)) return;

        if (isWearingHelmet(player) && isExposedToSun(player)) {
            if (player.getFireTicks() > 0 && !isRainingAtPlayer(player)) {
                player.setFireTicks(0);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isUndead(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyUndeadEffects(player), 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isUndead(player)) {
            removeUndeadEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isUndead(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyUndeadEffects(player), 5L);
        }
    }

    @EventHandler
    public void onInventoryCreativeBow(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (!isUndead(player)) return;

        ItemStack item = event.getCursor();
        if (item != null && isSpecialBow(item)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!isUndead(player)) return;

        ItemStack item = event.getItem();
        Material itemType = item.getType();

        if (itemType == Material.ROTTEN_FLESH) {
            event.setCancelled(true);

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getAmount() > 1) {
                mainHand.setAmount(mainHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            player.updateInventory();

            int saturation = plugin.getConfigManager().getUndeadRottenFleshSaturation();
            int newFood = Math.min(20, player.getFoodLevel() + saturation);
            player.setFoodLevel(newFood);
            player.setSaturation(Math.min(20, player.getSaturation() + (saturation * 0.6f)));
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.5f, 1.0f);
            return;
        }

        if (itemType == Material.SPIDER_EYE) {
            event.setCancelled(true);

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getAmount() > 1) {
                mainHand.setAmount(mainHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            player.updateInventory();

            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 2));
            player.setSaturation(Math.min(20, player.getSaturation() + 1.2f));
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.5f, 0.8f);
            return;
        }

        if (itemType.isEdible()) {
            event.setCancelled(true);

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getAmount() > 1) {
                mainHand.setAmount(mainHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            player.updateInventory();

            int newFoodLevel = Math.max(0, player.getFoodLevel() - 3);
            player.setFoodLevel(newFoodLevel);
            player.setSaturation(Math.max(0, player.getSaturation() - 2.0f));

            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 0.5f, 1.0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, false));
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        double healingMultiplier = plugin.getConfigManager().getUndeadHealingMultiplier();

        for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (!isUndead(player)) continue;

                for (PotionEffect effect : event.getPotion().getEffects()) {
                    if (effect.getType() == PotionEffectType.HUNGER) {
                        player.removePotionEffect(PotionEffectType.HUNGER);
                    }

                    if (effect.getType() == PotionEffectType.INSTANT_HEALTH) {
                        double damage = effect.getAmplifier() * 3.0 * healingMultiplier + 3.0 * healingMultiplier;
                        player.damage(damage);
                        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isUndead(player)) return;

        if (event.getNewEffect() != null && event.getNewEffect().getType() == PotionEffectType.HUNGER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;

        Player player = (Player) event.getTarget();
        if (!isUndead(player)) return;

        EntityType entityType = event.getEntity().getType();
        switch (entityType) {
            case ZOMBIE:
            case ZOMBIE_VILLAGER:
            case HUSK:
            case DROWNED:
            case SKELETON:
            case STRAY:
            case WITHER_SKELETON:
            case ZOMBIFIED_PIGLIN:
            case PHANTOM:
            case SLIME:
            case BOGGED:
                event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isUndead(player)) return;

        specialBows.remove(player.getUniqueId());

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isSpecialBow(item)) {
                iterator.remove();
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!isUndead(player)) return;

        if (isRainingAtPlayer(player) && player.getFireTicks() > 0) {
            player.setFireTicks(0);
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 3, 0.2, 0.5, 0.2, 0);
        }
    }

    private boolean isUndead(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}