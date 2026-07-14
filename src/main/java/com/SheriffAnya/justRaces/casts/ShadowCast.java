package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ShadowCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Тень";

    private final Map<UUID, Integer> invisibilityPotionTasks = new HashMap<>();
    private final Map<UUID, Integer> strengthTasks = new HashMap<>();
    private final Map<UUID, Integer> damageCount = new HashMap<>();
    private final Map<UUID, Integer> invisibilityParticleTasks = new HashMap<>();

    public ShadowCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyShadowEffects(Player player) {
        if (!isShadow(player)) {
            removeShadowEffects(player);
            return;
        }

        setShadowSize(player);
        setShadowHealth(player);
        startInvisibilityPotionTimer(player);
        startStrengthCheckTimer(player);
        startInvisibilityParticles(player);
        damageCount.put(player.getUniqueId(), 0);
    }

    public void removeShadowEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopInvisibilityPotionTimer(player);
        stopStrengthCheckTimer(player);
        stopInvisibilityParticles(player);
        restoreOriginalSize(player);
        restoreOriginalHealth(player);

        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.WEAKNESS);

        player.setFireTicks(0);

        damageCount.remove(playerId);
    }

    private void startInvisibilityParticles(Player player) {
        UUID playerId = player.getUniqueId();
        stopInvisibilityParticles(player);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isShadow(player)) {
                stopInvisibilityParticles(player);
                return;
            }

            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                // Тёмные частицы вокруг невидимого игрока
                player.getWorld().spawnParticle(Particle.WHITE_SMOKE,
                        player.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0.01);
            }
        }, 0L, 8L);

        invisibilityParticleTasks.put(playerId, taskId);
    }

    private void stopInvisibilityParticles(Player player) {
        UUID playerId = player.getUniqueId();
        if (invisibilityParticleTasks.containsKey(playerId)) {
            Integer taskId = invisibilityParticleTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            invisibilityParticleTasks.remove(playerId);
        }
    }

    private void setShadowSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Тени: " + e.getMessage());
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

    private void setShadowHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Тени: " + e.getMessage());
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

    private void startInvisibilityPotionTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopInvisibilityPotionTimer(player);

        int interval = 150 * 20;

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isShadow(player)) {
                stopInvisibilityPotionTimer(player);
                return;
            }

            boolean hasInvisibilityPotion = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && isInvisibilityPotion(item)) {
                    hasInvisibilityPotion = true;
                    break;
                }
            }

            if (!hasInvisibilityPotion) {
                giveInvisibilityPotion(player);
            }

        }, interval, interval);

        invisibilityPotionTasks.put(playerId, taskId);
    }

    private void stopInvisibilityPotionTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (invisibilityPotionTasks.containsKey(playerId)) {
            Integer taskId = invisibilityPotionTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            invisibilityPotionTasks.remove(playerId);
        }
    }

    private void startStrengthCheckTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopStrengthCheckTimer(player);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isShadow(player)) {
                stopStrengthCheckTimer(player);
                return;
            }

            boolean isInvisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY);

            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.WEAKNESS);

            if (isInvisible) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        100,
                        2,
                        false, false
                ));
                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
            } else {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS,
                        100,
                        1,
                        false, false
                ));
            }

        }, 0L, 20L);

        strengthTasks.put(playerId, taskId);
    }

    private void stopStrengthCheckTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (strengthTasks.containsKey(playerId)) {
            Integer taskId = strengthTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            strengthTasks.remove(playerId);
        }
    }

    private ItemStack createInvisibilityPotion() {
        ItemStack potion = new ItemStack(Material.POTION, 1);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§8Зелье невидимости Тени");
            meta.setLore(Arrays.asList(
                    "§7Длительность: 2 минуты",
                    "§7Выпадает каждые 2 минуты 30 секунд",
                    "§8※ Может выпить только раса Тень"
            ));

            meta.addCustomEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    120 * 20,
                    0,
                    false, false
            ), true);

            potion.setItemMeta(meta);
        }

        return potion;
    }

    private boolean isInvisibilityPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        if (!item.hasItemMeta()) return false;

        return item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().equals("§8Зелье невидимости Тени");
    }

    private void giveInvisibilityPotion(Player player) {
        ItemStack potion = createInvisibilityPotion();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(potion);

        if (!leftover.isEmpty()) {
            World world = player.getWorld();
            Location dropLoc = player.getLocation().add(0, 1, 0);
            world.dropItem(dropLoc, potion);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.0f);
        player.sendActionBar("§8Вы получили зелье невидимости!");
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isShadow(player)) return;

        int foodLevel = event.getFoodLevel();
        int currentFoodLevel = player.getFoodLevel();

        if (foodLevel < currentFoodLevel) {
            event.setFoodLevel(foodLevel - 1);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (!isShadow(player)) return;

        boolean isInvisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY);

        if (!isInvisible) {
            event.setDamage(event.getDamage() * 0.25);
        } else if (event.getEntity() instanceof LivingEntity) {
            // Частицы атаки из невидимости
            Location targetLoc = ((LivingEntity) event.getEntity()).getLocation();
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                    targetLoc.add(0, 1, 0), 1, 0, 0, 0, 0);
        }

        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.WITHER,
                    60,
                    0,
                    false, true
            ));
        } else if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.WITHER,
                    60,
                    0,
                    false, true
            ));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isShadow(player)) return;

        UUID playerId = player.getUniqueId();
        int dmgCount = damageCount.getOrDefault(playerId, 0);
        dmgCount++;
        damageCount.put(playerId, dmgCount);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isShadow(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyShadowEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isShadow(player)) {
            removeShadowEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isShadow(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyShadowEffects(player);
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isInvisibilityPotion(item)) {
            if (!isShadow(player)) {
                event.setCancelled(true);
                player.sendMessage("§cЭто зелье может выпить только игрок расы Тень!");
                player.updateInventory();
            } else {
                damageCount.put(player.getUniqueId(), 0);
                player.sendMessage("§8Вы выпили зелье невидимости Тени!");
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.5f, 1.0f);
            }
        }
    }

    private boolean isShadow(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}
