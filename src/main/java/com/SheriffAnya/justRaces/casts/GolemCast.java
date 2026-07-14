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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.*;
import org.bukkit.event.player.PlayerDropItemEvent;

public class GolemCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Голем";
    private final Map<UUID, ItemStack[]> ironArmor = new HashMap<>();
    private final Map<UUID, Integer> sparkParticleTasks = new HashMap<>();

    public GolemCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyGolemEffects(Player player) {
        if (!isGolem(player)) {
            removeGolemEffects(player);
            return;
        }

        setGolemSize(player);
        setGolemHealth(player);
        applyPermanentEffects(player);
        giveIronArmor(player);
        startSparkParticles(player);
    }

    public void removeGolemEffects(Player player) {
        UUID playerId = player.getUniqueId();

        restoreOriginalSize(player);
        restoreOriginalHealth(player);
        removeIronArmor(player);
        stopSparkParticles(player);

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.RESISTANCE);

        ironArmor.remove(playerId);
    }

    private void startSparkParticles(Player player) {
        UUID playerId = player.getUniqueId();
        stopSparkParticles(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isGolem(player)) {
                stopSparkParticles(player);
                return;
            }

            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    player.getLocation().add(0, 1, 0), 2, 0.4, 0.6, 0.4, 0.02);
        }, 0L, 15L);

        sparkParticleTasks.put(playerId, taskId);
    }

    private void stopSparkParticles(Player player) {
        UUID playerId = player.getUniqueId();
        if (sparkParticleTasks.containsKey(playerId)) {
            Integer taskId = sparkParticleTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            sparkParticleTasks.remove(playerId);
        }
    }

    private void setGolemSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Голема: " + e.getMessage());
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

    private void setGolemHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Голема: " + e.getMessage());
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
        boolean permanentSlowness = plugin.getConfigManager().isGolemPermanentSlowness();

        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        int resistanceLevel = (int) (plugin.getConfigManager().getGolemDamageResistance() * 10);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                Integer.MAX_VALUE,
                resistanceLevel,
                false,
                false
        ));

        if (permanentSlowness) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false
            ));
        }
    }

    // ===== ИЗМЕНЁННЫЙ МЕТОД: броня теперь имеет setUnbreakable(true) вместо зачарования =====
    private ItemStack createIronArmorPiece(Material material, String name) {
        ItemStack armor = new ItemStack(material, 1);
        ItemMeta meta = armor.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                    "§7Броня Голема",
                    "§7Нельзя снять или сломать"
            ));

            // Устанавливаем флаг неразрушимости вместо зачарования
            meta.setUnbreakable(true);
            // Зачарование "Неразрушимость" для отображения в тултипе
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            // Скрываем числовую прочность из тултипа
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            // Добавляем проклятие привязанности, чтобы нельзя было снять
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);

            armor.setItemMeta(meta);
        }

        return armor;
    }

    private void giveIronArmor(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerInventory inventory = player.getInventory();

        ItemStack helmet = createIronArmorPiece(Material.IRON_HELMET, "§7Шлем Голема");
        ItemStack chestplate = createIronArmorPiece(Material.IRON_CHESTPLATE, "§7Нагрудник Голема");
        ItemStack leggings = createIronArmorPiece(Material.IRON_LEGGINGS, "§7Поножи Голема");
        ItemStack boots = createIronArmorPiece(Material.IRON_BOOTS, "§7Ботинки Голема");

        ItemStack[] currentArmor = inventory.getArmorContents();
        ironArmor.put(playerId, currentArmor);

        inventory.setHelmet(helmet);
        inventory.setChestplate(chestplate);
        inventory.setLeggings(leggings);
        inventory.setBoots(boots);

        player.updateInventory();
    }

    private void removeIronArmor(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerInventory inventory = player.getInventory();

        if (ironArmor.containsKey(playerId)) {
            ItemStack[] originalArmor = ironArmor.get(playerId);
            inventory.setArmorContents(originalArmor);
        } else {
            inventory.setHelmet(null);
            inventory.setChestplate(null);
            inventory.setLeggings(null);
            inventory.setBoots(null);
        }

        player.updateInventory();
    }

    private boolean isIronArmor(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();
        return type == Material.IRON_HELMET ||
                type == Material.IRON_CHESTPLATE ||
                type == Material.IRON_LEGGINGS ||
                type == Material.IRON_BOOTS;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isGolem(player)) return;

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null && isIronArmor(currentItem)) {
                event.setCancelled(true);
                player.updateInventory();
                player.sendMessage("§cВы не можете снять броню Голема!");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isGolem(player)) return;

        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isIronArmor(droppedItem)) {
            event.setCancelled(true);
            player.updateInventory();
            player.sendMessage("§cВы не можете выбросить броню Голема!");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        boolean noKnockback = plugin.getConfigManager().isGolemNoKnockback();
        boolean knockbackAttacker = plugin.getConfigManager().isGolemKnockbackAttacker();
        double knockbackPower = plugin.getConfigManager().getGolemKnockbackPower();

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!isGolem(player)) return;

            if (noKnockback) {
                player.setVelocity(new Vector(0, 0, 0));
                spawnBlockCrackParticles(player);
            }

            if (knockbackAttacker && event.getDamager() instanceof LivingEntity) {
                LivingEntity damager = (LivingEntity) event.getDamager();

                Vector direction = damager.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize();

                damager.setVelocity(direction.multiply(knockbackPower).setY(0.5));
            }
        }

        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (!isGolem(player)) return;
        }
    }

    private void spawnBlockCrackParticles(Player player) {
        player.getWorld().spawnParticle(Particle.BLOCK,
                player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, 0,
                Material.IRON_BLOCK.createBlockData());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isGolem(player)) return;

        boolean noKnockback = plugin.getConfigManager().isGolemNoKnockback();

        if (noKnockback && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE)) {
            player.setVelocity(new Vector(0, 0, 0));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isGolem(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyGolemEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isGolem(player)) {
            removeGolemEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isGolem(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyGolemEffects(player);
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isGolem(player)) return;

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isIronArmor(item)) {
                iterator.remove();
            }
        }
    }

    private boolean isGolem(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}