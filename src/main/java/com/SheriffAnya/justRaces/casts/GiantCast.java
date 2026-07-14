package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class GiantCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Гигант";

    private final Map<UUID, Integer> miningTasks = new HashMap<>();

    public GiantCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyGiantEffects(Player player) {
        if (!isGiant(player)) {
            removeGiantEffects(player);
            return;
        }

        setGiantSize(player);
        setGiantHealth(player);
        startMiningSpeedCheck(player);
    }

    public void removeGiantEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopMiningSpeedCheck(player);
        restoreOriginalSize(player);
        restoreOriginalHealth(player);
    }

    private void setGiantSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Гиганта: " + e.getMessage());
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

    private void setGiantHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Гиганта: " + e.getMessage());
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

    private void startMiningSpeedCheck(Player player) {
        UUID playerId = player.getUniqueId();
        stopMiningSpeedCheck(player);

        boolean permanentSlowness = plugin.getConfigManager().isGiantPermanentSlowness();

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isGiant(player)) {
                stopMiningSpeedCheck(player);
                return;
            }

            if (permanentSlowness) {
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        100,
                        0,
                        false,
                        false
                ));
            }

            player.removePotionEffect(PotionEffectType.HASTE);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE,
                    100,
                    0,
                    false,
                    false
            ));

        }, 0L, 80L);

        miningTasks.put(playerId, taskId);
    }

    private void stopMiningSpeedCheck(Player player) {
        UUID playerId = player.getUniqueId();
        if (miningTasks.containsKey(playerId)) {
            Integer taskId = miningTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            miningTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.HASTE);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isGiant(player)) return;

        player.getWorld().spawnParticle(Particle.CRIT,
                event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                10, 0.3, 0.3, 0.3, 0.1);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (!isGiant(player)) return;

        boolean isFistAttack = player.getInventory().getItemInMainHand().getType() == Material.AIR;

        if (isFistAttack) {
            double fistDamage = plugin.getConfigManager().getGiantFistDamage();
            event.setDamage(fistDamage);

            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();

                Vector direction = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize();

                double knockbackPower = plugin.getConfigManager().getGiantKnockbackPower();
                target.setVelocity(direction.multiply(knockbackPower).setY(0.8));
            }

            player.getWorld().playSound(player.getLocation(),
                    Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                    1.0f, 0.8f);
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        if (!isGiant(player)) return;

        boolean noBedSleeping = plugin.getConfigManager().isGiantNoBedSleeping();
        if (!noBedSleeping) return; // сон на одиночной кровати разрешён

        Block bed = event.getBed();

        // Разрешаем сон, если кровати образуют 2×2 (4 блока кровати в квадрате)
        if (isDoubleBed(bed)) return;

        event.setCancelled(true);
        player.sendMessage("§cГиганты слишком большие для одиночной кровати! Нужна кровать 2×2.");
    }

    /**
     * Проверяет, является ли данный блок кровати частью квадрата 2×2
     * (т.е. четыре блока кровати образуют прямоугольник 2×2).
     * Кровать в Minecraft занимает 2 блока (HEAD и FOOT).
     * Две кровати рядом образуют квадрат 2×2 из 4 блоков.
     */
    private boolean isDoubleBed(Block bed) {
        if (!(bed.getBlockData() instanceof Bed)) return false;
        Bed bedData = (Bed) bed.getBlockData();

        // Получаем блок с головой (HEAD) кровати, чтобы работать с фиксированной точкой
        Block head;
        Block foot;
        if (bedData.getPart() == Bed.Part.HEAD) {
            head = bed;
            foot = bed.getRelative(bedData.getFacing().getOppositeFace());
        } else {
            foot = bed;
            head = bed.getRelative(bedData.getFacing());
        }

        // Направление кровати: ось HEAD→FOOT
        BlockFace facing = bedData.getFacing(); // направление от HEAD к FOOT

        // Перпендикулярные оси
        BlockFace[] sides;
        if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
            sides = new BlockFace[]{BlockFace.EAST, BlockFace.WEST};
        } else {
            sides = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH};
        }

        for (BlockFace side : sides) {
            Block neighborHead = head.getRelative(side);
            Block neighborFoot = foot.getRelative(side);

            if (!(neighborHead.getBlockData() instanceof Bed)) continue;
            if (!(neighborFoot.getBlockData() instanceof Bed)) continue;

            Bed nbHead = (Bed) neighborHead.getBlockData();
            Bed nbFoot = (Bed) neighborFoot.getBlockData();

            // Соседние блоки должны быть головой и ногами кровати, параллельной текущей
            if (nbHead.getPart() == Bed.Part.HEAD &&
                    nbFoot.getPart() == Bed.Part.FOOT &&
                    nbHead.getFacing() == facing) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isGiant(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyGiantEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isGiant(player)) {
            removeGiantEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isGiant(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyGiantEffects(player);
            }, 5L);
        }
    }

    private boolean isGiant(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}
