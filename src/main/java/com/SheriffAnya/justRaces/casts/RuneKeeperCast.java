package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RuneKeeperCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "ХранительРун";
    private final Map<UUID, Integer> runeCount = new HashMap<>();
    private final Map<UUID, Integer> runeRegenTasks = new HashMap<>();
    private final Map<UUID, BossBar> runeBossBars = new HashMap<>();
    private final Map<UUID, Integer> stageCheckTasks = new HashMap<>();
    private final Map<UUID, Integer> currentStage = new HashMap<>();
    private final Map<UUID, Integer> particleTasks = new HashMap<>();
    private final Random random = new Random();

    public RuneKeeperCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyRuneKeeperEffects(Player player) {
        if (!isRuneKeeper(player)) {
            removeRuneKeeperEffects(player);
            return;
        }

        UUID playerId = player.getUniqueId();

        int maxRunes = plugin.getConfigManager().getRuneKeeperMaxRunes();
        if (!runeCount.containsKey(playerId)) {
            runeCount.put(playerId, maxRunes);
        }

        setRuneKeeperSize(player);
        updatePlayerStage(player);
        applyStageEffects(player);
        startRuneRegeneration(player);
        startStageCheck(player);
        startParticleEffect(player);
        updateNightVision(player);
        updateRuneBossBar(player);

        plugin.getArmorManager().removeForbiddenItems(player);
    }

    public void removeRuneKeeperEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopRuneRegeneration(player);
        stopStageCheck(player);
        stopParticleEffect(player);
        restoreOriginalStats(player);

        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);

        runeCount.remove(playerId);
        currentStage.remove(playerId);

        if (runeBossBars.containsKey(playerId)) {
            runeBossBars.get(playerId).removeAll();
            runeBossBars.remove(playerId);
        }
    }

    private void setRuneKeeperSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Хранителя Рун: " + e.getMessage());
        }
    }

    private void startStageCheck(Player player) {
        UUID playerId = player.getUniqueId();
        stopStageCheck(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRuneKeeper(player)) {
                stopStageCheck(player);
                return;
            }

            updatePlayerStage(player);
            applyStageEffects(player);

        }, 0L, 20L);

        stageCheckTasks.put(playerId, taskId);
    }

    private void stopStageCheck(Player player) {
        UUID playerId = player.getUniqueId();
        if (stageCheckTasks.containsKey(playerId)) {
            Integer taskId = stageCheckTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            stageCheckTasks.remove(playerId);
        }
    }

    private void updatePlayerStage(Player player) {
        UUID playerId = player.getUniqueId();
        int playerLevel = player.getLevel();
        int stage = 0;

        int[] levelStages = {5, 10, 15, 20, 25, 30};

        for (int i = levelStages.length - 1; i >= 0; i--) {
            if (playerLevel >= levelStages[i]) {
                stage = i;
                break;
            }
        }

        int oldStage = currentStage.getOrDefault(playerId, -1);
        if (oldStage != stage) {
            currentStage.put(playerId, stage);
            player.sendMessage("§5§lХранитель Рун достиг " + levelStages[stage] + " уровня! Сила возрастает!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Вспышка частиц при активации новой стадии (только если это не первая инициализация)
            if (oldStage != -1) {
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        player.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.5);
            }
        }
    }

    private void applyStageEffects(Player player) {
        UUID playerId = player.getUniqueId();
        int stage = currentStage.getOrDefault(playerId, 0);

        double stageHealth = plugin.getConfigManager().getRuneKeeperStageHealth(stage);
        int stageSpeed = plugin.getConfigManager().getRuneKeeperStageSpeed(stage);
        double stageJump = plugin.getConfigManager().getRuneKeeperStageJump(stage);
        double stageSize = plugin.getConfigManager().getRuneKeeperStageSize(stage);

        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double oldHealth = maxHealthAttr.getBaseValue();
                if (Math.abs(oldHealth - stageHealth) > 0.01) {
                    double healthPercent = player.getHealth() / oldHealth;
                    maxHealthAttr.setBaseValue(stageHealth);
                    player.setHealth(Math.min(stageHealth, stageHealth * healthPercent));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка установки здоровья Хранителя Рун: " + e.getMessage());
        }

        try {
            float newSize = (float) stageSize;
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                if (Math.abs(scaleAttr.getBaseValue() - newSize) > 0.01) {
                    scaleAttr.setBaseValue(newSize);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка установки размера Хранителя Рун: " + e.getMessage());
        }

        player.removePotionEffect(PotionEffectType.SPEED);
        if (stageSpeed > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    Integer.MAX_VALUE,
                    stageSpeed - 1,
                    false,
                    false
            ));
        }

        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        if (stageJump > 1.0) {
            int jumpLevel = (int) ((stageJump - 1.0) * 2);
            if (jumpLevel > 0) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.JUMP_BOOST,
                        Integer.MAX_VALUE,
                        jumpLevel - 1,
                        false,
                        false
                ));
            }
        }
    }

    private void restoreOriginalStats(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double healthPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * healthPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка восстановления характеристик: " + e.getMessage());
        }
    }

    private void startRuneRegeneration(Player player) {
        UUID playerId = player.getUniqueId();
        stopRuneRegeneration(player);

        int interval = plugin.getConfigManager().getRuneKeeperRuneRegenInterval() * 20;
        int maxRunes = plugin.getConfigManager().getRuneKeeperMaxRunes();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRuneKeeper(player)) {
                stopRuneRegeneration(player);
                return;
            }

            int currentRunes = runeCount.getOrDefault(playerId, maxRunes);
            if (currentRunes < maxRunes) {
                runeCount.put(playerId, currentRunes + 1);
                updateRuneBossBar(player);
                updateNightVision(player);

                player.playSound(player.getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f);
                player.sendActionBar("§d+1 Руна! [" + (currentRunes + 1) + "/" + maxRunes + "]");
            }

        }, interval, interval);

        runeRegenTasks.put(playerId, taskId);
    }

    private void stopRuneRegeneration(Player player) {
        UUID playerId = player.getUniqueId();
        if (runeRegenTasks.containsKey(playerId)) {
            Integer taskId = runeRegenTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            runeRegenTasks.remove(playerId);
        }
    }

    private void startParticleEffect(Player player) {
        UUID playerId = player.getUniqueId();
        stopParticleEffect(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isRuneKeeper(player)) {
                stopParticleEffect(player);
                return;
            }

            int runes = runeCount.getOrDefault(playerId, 0);
            Location loc = player.getLocation().add(0, 1, 0);

            if (runes > 0) {
                for (int i = 0; i < runes; i++) {
                    double angle = (2 * Math.PI / runes) * i + (System.currentTimeMillis() / 500.0);
                    double x = Math.cos(angle) * 0.8;
                    double z = Math.sin(angle) * 0.8;
                    Location particleLoc = loc.clone().add(x, 0.2, z);

                    player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                    if (i % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }

        }, 0L, 5L);

        particleTasks.put(playerId, taskId);
    }

    private void stopParticleEffect(Player player) {
        UUID playerId = player.getUniqueId();
        if (particleTasks.containsKey(playerId)) {
            Integer taskId = particleTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            particleTasks.remove(playerId);
        }
    }

    private void updateRuneBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        int runes = runeCount.getOrDefault(playerId, 0);
        int maxRunes = plugin.getConfigManager().getRuneKeeperMaxRunes();

        boolean showBossbar = plugin.getConfigManager().isRuneKeeperShowBossbar();
        if (!showBossbar) return;

        if (!runeBossBars.containsKey(playerId)) {
            BossBar bossBar = Bukkit.createBossBar(
                    "§dРуны Хранителя",
                    BarColor.PURPLE,
                    BarStyle.SEGMENTED_6
            );
            bossBar.addPlayer(player);
            runeBossBars.put(playerId, bossBar);
        }

        BossBar bossBar = runeBossBars.get(playerId);
        bossBar.setProgress((double) runes / maxRunes);
        bossBar.setTitle("§dРуны: " + runes + "/" + maxRunes);
    }

    private void updateNightVision(Player player) {
        UUID playerId = player.getUniqueId();
        int runes = runeCount.getOrDefault(playerId, 0);
        int maxRunes = plugin.getConfigManager().getRuneKeeperMaxRunes();
        boolean nightVisionWithRunes = plugin.getConfigManager().isRuneKeeperNightVisionWithRunes();

        player.removePotionEffect(PotionEffectType.NIGHT_VISION);

        if (nightVisionWithRunes && runes == maxRunes) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false
            ));
        }
    }


    /**
     * Синхронизирует полоску опыта с реальным значением totalExperience.
     * player.setTotalExperience() меняет внутренний счётчик, но не обновляет
     * отображаемый уровень и прогресс на клиенте — нужно выставить их вручную.
     */
    private void syncExpBar(Player player) {
        int total = player.getTotalExperience();
        int level = 0;
        int remaining = total;

        // Таблица опыта до следующего уровня (ванильные формулы)
        while (true) {
            int needed;
            if (level < 16)       needed = 2 * level + 7;
            else if (level < 31)  needed = 5 * level - 38;
            else                  needed = 9 * level - 158;

            if (remaining < needed) break;
            remaining -= needed;
            level++;
        }

        int needed;
        if (level < 16)       needed = 2 * level + 7;
        else if (level < 31)  needed = 5 * level - 38;
        else                  needed = 9 * level - 158;

        player.setLevel(level);
        player.setExp(needed > 0 ? (float) remaining / needed : 0f);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (!isRuneKeeper(player)) return;

        UUID playerId = player.getUniqueId();
        int runes = runeCount.getOrDefault(playerId, 0);
        int maxRunes = plugin.getConfigManager().getRuneKeeperMaxRunes();
        double critMultiplier = plugin.getConfigManager().getRuneKeeperCritMultiplier();

        if (runes == maxRunes) {
            event.setDamage(event.getDamage() * critMultiplier);
        }

        if (event.isCritical()) {
            int expToRemove = 7 + random.nextInt(4);
            int currentTotal = player.getTotalExperience();
            int newTotal = Math.max(0, currentTotal - expToRemove);
            player.setTotalExperience(newTotal);
            syncExpBar(player);
            player.sendActionBar("§c-" + expToRemove + " опыта (крит)");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isRuneKeeper(player)) return;

        UUID playerId = player.getUniqueId();
        int runes = runeCount.getOrDefault(playerId, 0);

        if (runes > 0) {
            runeCount.put(playerId, runes - 1);
            event.setCancelled(true);

            updateRuneBossBar(player);
            updateNightVision(player);

            player.getWorld().playSound(player.getLocation(),
                    Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.ENCHANT,
                    player.getLocation().add(0, 1, 0),
                    20, 0.5, 0.5, 0.5, 1.0);

            player.sendActionBar("§dРуна поглотила урон! Осталось рун: " + (runes - 1));
        } else {
            int expToRemove = 5 + random.nextInt(16);
            int currentTotal = player.getTotalExperience();
            int newTotal = Math.max(0, currentTotal - expToRemove);
            player.setTotalExperience(newTotal);
            syncExpBar(player);
            player.sendActionBar("§c-" + expToRemove + " опыта (нет рун)");
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (!isRuneKeeper(player)) return;
        updatePlayerStage(player);
        applyStageEffects(player);
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        if (!isRuneKeeper(player)) return;
        updatePlayerStage(player);
        applyStageEffects(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isRuneKeeper(player)) return;

        boolean noShields = plugin.getConfigManager().isRuneKeeperNoShields();
        boolean noTotems = plugin.getConfigManager().isRuneKeeperNoTotems();

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if ((currentItem != null && isArmor(currentItem)) || (cursorItem != null && isArmor(cursorItem))) {
                event.setCancelled(true);
                player.sendMessage("§cХранитель Рун не может носить броню!");
                player.updateInventory();
                return;
            }
        }

        boolean shieldInvolved = (currentItem != null && currentItem.getType() == Material.SHIELD) ||
                (cursorItem != null && cursorItem.getType() == Material.SHIELD);
        boolean totemInvolved = (currentItem != null && currentItem.getType() == Material.TOTEM_OF_UNDYING) ||
                (cursorItem != null && cursorItem.getType() == Material.TOTEM_OF_UNDYING);

        if (noShields && shieldInvolved) {
            event.setCancelled(true);
            player.sendMessage("§cХранитель Рун не может использовать щиты!");
            player.updateInventory();
            return;
        }

        if (noTotems && totemInvolved) {
            event.setCancelled(true);
            player.sendMessage("§cХранитель Рун не может использовать тотемы!");
            player.updateInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isRuneKeeper(player)) return;

        boolean noShields = plugin.getConfigManager().isRuneKeeperNoShields();
        boolean noTotems = plugin.getConfigManager().isRuneKeeperNoTotems();

        ItemStack dragged = event.getOldCursor();
        if (dragged == null) return;

        boolean forbidden = isArmor(dragged) ||
                (noShields && dragged.getType() == Material.SHIELD) ||
                (noTotems && dragged.getType() == Material.TOTEM_OF_UNDYING);

        if (forbidden) {
            event.setCancelled(true);
            player.sendMessage("§cХранитель Рун не может использовать этот предмет!");
            player.updateInventory();
        }
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isRuneKeeper(player)) return;

        boolean noShields = plugin.getConfigManager().isRuneKeeperNoShields();
        boolean noTotems = plugin.getConfigManager().isRuneKeeperNoTotems();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean shieldInHand = (mainHand != null && mainHand.getType() == Material.SHIELD) ||
                (offHand != null && offHand.getType() == Material.SHIELD);
        boolean totemInHand = (mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING) ||
                (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING);

        if ((noShields && shieldInHand) || (noTotems && totemInHand)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> removeRuneKeeperForbiddenItems(player));
        }
    }

    private void removeRuneKeeperForbiddenItems(Player player) {
        boolean noShields = plugin.getConfigManager().isRuneKeeperNoShields();
        boolean noTotems = plugin.getConfigManager().isRuneKeeperNoTotems();

        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (isArmor(item) ||
                    (noShields && item.getType() == Material.SHIELD) ||
                    (noTotems && item.getType() == Material.TOTEM_OF_UNDYING)) {
                player.getWorld().dropItem(player.getLocation(), item.clone());
                inv.setItem(i, null);
            }
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (offHand != null && ((noShields && offHand.getType() == Material.SHIELD) ||
                (noTotems && offHand.getType() == Material.TOTEM_OF_UNDYING))) {
            player.getWorld().dropItem(player.getLocation(), offHand.clone());
            inv.setItemInOffHand(null);
        }

        player.updateInventory();
    }

    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type.name().endsWith("_HELMET") ||
                type.name().endsWith("_CHESTPLATE") ||
                type.name().endsWith("_LEGGINGS") ||
                type.name().endsWith("_BOOTS");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isRuneKeeper(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyRuneKeeperEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isRuneKeeper(player)) {
            removeRuneKeeperEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isRuneKeeper(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyRuneKeeperEffects(player);
            }, 5L);
        }
    }

    private boolean isRuneKeeper(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}
