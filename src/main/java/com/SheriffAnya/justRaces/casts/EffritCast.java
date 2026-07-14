package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EffritCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Эфрит";

    private final Map<UUID, Integer> waterDamageTasks = new HashMap<>();
    private final Map<UUID, Integer> fireBuffTasks = new HashMap<>();
    private final Map<UUID, ItemStack> fireballItems = new HashMap<>();
    private final Map<UUID, Long> fireballCooldowns = new HashMap<>();
    private static final String FIREBALL_NAME = "§6Огненный шар Эфрита";

    public EffritCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyEffritEffects(Player player) {
        if (!isEffrit(player)) {
            removeEffritEffects(player);
            return;
        }

        setEffritSize(player);
        setEffritHealth(player);
        startFireBuffTimer(player);
        startWaterDamageTimer(player);
        giveSpecialFireball(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }

    public void removeEffritEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopWaterDamageTimer(player);
        stopFireBuffTimer(player);
        restoreOriginalSize(player);
        restoreOriginalHealth(player);

        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.REGENERATION);

        fireballItems.remove(playerId);
        fireballCooldowns.remove(playerId);
        removeFireball(player);
    }

    private void setEffritSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Эфрита: " + e.getMessage());
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

    private void setEffritHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Эфрита: " + e.getMessage());
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

    private void startWaterDamageTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopWaterDamageTimer(player);

        double drowningDamage = plugin.getConfigManager().getRusalDrowningDamage();

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isEffrit(player)) {
                    stopWaterDamageTimer(player);
                    return;
                }

                if (!isInNether(player) && (isInWater(player) || isInRain(player))) {
                    player.damage(drowningDamage);
                    player.getWorld().spawnParticle(Particle.SMOKE,
                            player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 1.0f);
                }
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

    private void startFireBuffTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopFireBuffTimer(player);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isEffrit(player)) {
                    stopFireBuffTimer(player);
                    return;
                }

                if (isInNether(player)) {
                    applyNetherEffects(player);
                } else {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.removePotionEffect(PotionEffectType.SPEED);
                    player.removePotionEffect(PotionEffectType.STRENGTH);
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                }
                giveSpecialFireball(player);
            }
        }, 0L, 40L);

        fireBuffTasks.put(playerId, taskId);
    }

    private void applyNetherEffects(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, 400, 0, false, false
            ));
        }

        if (!player.hasPotionEffect(PotionEffectType.SPEED) ||
                player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() < 1) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 200, 1, false, false
            ));
        }

        if (!player.hasPotionEffect(PotionEffectType.STRENGTH)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH, 200, 0, false, false
            ));
        }

        if (player.getFireTicks() > 0 || isInLava(player)) {
            if (!player.hasPotionEffect(PotionEffectType.REGENERATION)) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.REGENERATION, 100, 0, false, false
                ));
            }
        }
    }

    private void stopFireBuffTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (fireBuffTasks.containsKey(playerId)) {
            Integer taskId = fireBuffTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            fireBuffTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }

    private ItemStack createSpecialFireball() {
        ItemStack fireball = new ItemStack(Material.FIRE_CHARGE, 1);
        ItemMeta meta = fireball.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(FIREBALL_NAME);
            meta.setLore(Arrays.asList(
                    "§7Особый предмет Эфрита",
                    "§7ПКМ: выстрел",
                    "§7В аду: -2 голода, КД 5с",
                    "§7В других мирах: -6 голода, КД 5с"
            ));
            meta.setUnbreakable(true);
            fireball.setItemMeta(meta);
        }

        return fireball;
    }

    private boolean isSpecialFireball(ItemStack item) {
        if (item == null || item.getType() != Material.FIRE_CHARGE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() &&
                meta.getDisplayName().equals(FIREBALL_NAME);
    }

    private void giveSpecialFireball(Player player) {
        UUID playerId = player.getUniqueId();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSpecialFireball(item)) {
                fireballItems.put(playerId, item);
                return;
            }
        }

        ItemStack fireball = createSpecialFireball();
        fireballItems.put(playerId, fireball);

        boolean placed = false;
        for (int i = 0; i < 9; i++) {
            ItemStack slotItem = player.getInventory().getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                player.getInventory().setItem(i, fireball);
                placed = true;
                break;
            }
        }

        if (!placed) {
            player.getInventory().addItem(fireball);
        }

        player.updateInventory();
    }

    private void removeFireball(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isSpecialFireball(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
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

    private boolean isInLava(Player player) {
        Location loc = player.getLocation();
        Material blockAtFeet = loc.getBlock().getType();

        return blockAtFeet == Material.LAVA ||
                blockAtFeet == Material.LAVA_CAULDRON ||
                player.isInLava();
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

    private boolean isInNether(Player player) {
        return player.getWorld().getEnvironment() == World.Environment.NETHER;
    }

    private boolean isEffrit(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }


    // Адские монстры игнорируют Ифрита (не атакуют его),
    // кроме ZombifiedPiglin — он агрессивен если его ударить.
    private static final Set<Class<? extends LivingEntity>> NETHER_MOBS = new HashSet<>(Arrays.asList(
            Blaze.class,
            Ghast.class,
            MagmaCube.class,
            Piglin.class,
            PiglinBrute.class,
            Hoglin.class,
            Zoglin.class,
            WitherSkeleton.class
    ));

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNetherMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        Player target = (Player) event.getTarget();
        if (!isEffrit(target)) return;

        Entity mob = event.getEntity();
        // ZombifiedPiglin — исключение: если его ударил игрок, он агрессивен — не отменяем
        if (mob instanceof PigZombie) return;

        for (Class<? extends LivingEntity> clazz : NETHER_MOBS) {
            if (clazz.isInstance(mob)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNetherMobAttackEffrit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!isEffrit(player)) return;

        Entity attacker = event.getDamager();
        if (attacker instanceof PigZombie) return;

        for (Class<? extends LivingEntity> clazz : NETHER_MOBS) {
            if (clazz.isInstance(attacker)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isEffrit(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyEffritEffects(player);
                }
            }.runTaskLater(plugin, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isEffrit(player)) {
            removeEffritEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isEffrit(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyEffritEffects(player);
                    if (!isInNether(player)) {
                        player.sendMessage("§cВы возродились не в аду! Эфрит слабеет...");
                    } else {
                        player.sendMessage("§cВы возродились в аду! Эфрит чувствует силу!");
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!isEffrit(player)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                applyEffritEffects(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isEffrit(player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {

            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isEffrit(player)) return;

        ItemStack item = event.getItem();
        if (item == null || !isSpecialFireball(item)) return;

        if (!event.getAction().toString().contains("RIGHT")) return;

        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (fireballCooldowns.containsKey(playerId)) {
            long cooldownEnd = fireballCooldowns.get(playerId);
            if (currentTime < cooldownEnd) {
                long remaining = (cooldownEnd - currentTime) / 1000;
                player.sendActionBar("§cОгненный шар перезаряжается: " + remaining + "с");
                return;
            }
        }

        boolean inNether = isInNether(player);
        int foodCost = inNether ? 2 : 6;

        if (player.getFoodLevel() < foodCost) {
            player.sendActionBar("§cНедостаточно голода! Нужно " + foodCost + " голода.");
            return;
        }

        player.setFoodLevel(player.getFoodLevel() - foodCost);
        fireballCooldowns.put(playerId, currentTime + 5000L);

        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        SmallFireball fireball = player.getWorld().spawn(eyeLocation.add(direction), SmallFireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(direction.multiply(2.0));
        fireball.setYield(2.0f);
        fireball.setIsIncendiary(true);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
        player.getWorld().spawnParticle(Particle.FLAME, eyeLocation, 20, 0.1, 0.1, 0.1, 0.1);

        if (inNether) {
            player.sendActionBar("§aОгненный шар выпущен! (-2 голода)");
        } else {
            player.sendActionBar("§aОгненный шар выпущен! (-6 голода)");
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isEffrit(player)) return;

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isSpecialFireball(item)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof SmallFireball)) return;

        SmallFireball fireball = (SmallFireball) event.getEntity();
        if (!(fireball.getShooter() instanceof Player)) return;

        Player player = (Player) fireball.getShooter();
        if (!isEffrit(player)) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (!isSpecialFireball(itemInHand)) {
            if (itemInHand != null && itemInHand.getType() == Material.FIRE_CHARGE) {
                event.setCancelled(true);
                player.sendActionBar("§cИспользуйте особый огненный шар Эфрита!");
                player.updateInventory();
            }
        }
    }
}
