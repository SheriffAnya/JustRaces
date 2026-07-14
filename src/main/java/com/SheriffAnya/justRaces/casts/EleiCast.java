package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class EleiCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Элей";

    private static final String FEATHER_NAME = "§eВолшебное перо Элея";
    private static final String WIND_CHARGE_NAME = "§bЗаряд ветра";

    private final Map<UUID, Integer> flightTimerTasks = new HashMap<>();
    private final Map<UUID, Integer> usedFlightTime = new HashMap<>();
    private final Map<UUID, Integer> actionBarTasks = new HashMap<>();
    private final Map<UUID, ItemStack> featherItems = new HashMap<>();
    private final Map<UUID, Integer> speedTasks = new HashMap<>();
    private final Map<UUID, Boolean> isFlying = new HashMap<>();
    private final Map<UUID, Integer> windChargeTasks = new HashMap<>();
    private final Map<UUID, Long> cooldownEndTime = new HashMap<>();
    private final Map<UUID, Long> featherCooldown = new HashMap<>();

    private final Map<UUID, Long> pvpCooldownEndTime = new HashMap<>();
    private final Map<UUID, Boolean> isInPvpMode = new HashMap<>();
    private final Map<UUID, Integer> pvpModeTimerTasks = new HashMap<>();
    private final Map<UUID, Integer> flightParticleTasks = new HashMap<>();

    public EleiCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyEleiEffects(Player player) {
        if (!isElei(player)) {
            removeEleiEffects(player);
            return;
        }

        UUID playerId = player.getUniqueId();

        setEleiSize(player);
        setEleiHealth(player);
        startSpeedEffect(player);
        startActionBar(player);
        startWindChargeTimer(player);
        startPvpModeTimer(player);
        giveMagicFeather(player);

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            player.setAllowFlight(true);
            player.setFlying(true);
            isFlying.put(playerId, true);
        } else {
            isFlying.put(playerId, false);
        }
    }

    public void removeEleiEffects(Player player) {
        UUID playerId = player.getUniqueId();

        forceStopFlight(player);
        stopSpeedEffect(player);
        stopActionBar(player);
        stopWindChargeTimer(player);
        stopPvpModeTimer(player);
        restoreOriginalSize(player);
        restoreOriginalHealth(player);
        player.removePotionEffect(PotionEffectType.SPEED);
        removeMagicFeather(player);
        removeAllWindCharges(player);

        usedFlightTime.remove(playerId);
        cooldownEndTime.remove(playerId);
        featherCooldown.remove(playerId);
        isFlying.remove(playerId);
        featherItems.remove(playerId);

        pvpCooldownEndTime.remove(playerId);
        isInPvpMode.remove(playerId);
    }

    /**
     * Независимый таймер, который гарантирует сброс PVP-режима по истечении
     * кулдауна, даже если игрок не летает и action bar отключён в конфиге.
     * Баг: ранее updatePvpMode() вызывался только из таймера полёта или
     * action bar, поэтому при отключённом action bar и отсутствии полёта
     * PVP-режим мог остаться включённым навсегда.
     */
    private void startPvpModeTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopPvpModeTimer(player);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isElei(player)) {
                stopPvpModeTimer(player);
                return;
            }
            updatePvpMode(player);
        }, 20L, 20L);

        pvpModeTimerTasks.put(playerId, taskId);
    }

    private void stopPvpModeTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (pvpModeTimerTasks.containsKey(playerId)) {
            Integer taskId = pvpModeTimerTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            pvpModeTimerTasks.remove(playerId);
        }
    }

    private void setEleiSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Элей: " + e.getMessage());
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

    private void setEleiHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Элей: " + e.getMessage());
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

    private double getPvpFlightDrainMultiplier() {
        return plugin.getConfigManager().getEleiPvpFlightDrainMultiplier();
    }

    private double getPvpFlightSpeedMultiplier() {
        return plugin.getConfigManager().getEleiPvpFlightSpeedMultiplier();
    }

    private int getPvpCooldownSeconds() {
        return plugin.getConfigManager().getEleiPvpCooldown();
    }

    private boolean isInPvpMode(Player player) {
        UUID playerId = player.getUniqueId();
        return isInPvpMode.getOrDefault(playerId, false);
    }

    private void activatePvpMode(Player player) {
        UUID playerId = player.getUniqueId();

        long currentTime = System.currentTimeMillis();
        long cooldownEnd = currentTime + (getPvpCooldownSeconds() * 1000L);

        pvpCooldownEndTime.put(playerId, cooldownEnd);
        isInPvpMode.put(playerId, true);

        if (isFlying.getOrDefault(playerId, false) && player.isFlying()) {
            applyFlightSpeedModifier(player);
        }

        player.sendActionBar("§c⚔ PVP режим активирован на " + getPvpCooldownSeconds() + " сек!");
    }

    private void updatePvpMode(Player player) {
        UUID playerId = player.getUniqueId();

        if (isInPvpMode.getOrDefault(playerId, false)) {
            long currentTime = System.currentTimeMillis();
            Long cooldownEnd = pvpCooldownEndTime.get(playerId);

            if (cooldownEnd == null || currentTime >= cooldownEnd) {
                isInPvpMode.put(playerId, false);
                pvpCooldownEndTime.remove(playerId);

                if (isFlying.getOrDefault(playerId, false) && player.isFlying()) {
                    applyFlightSpeedModifier(player);
                }

                player.sendActionBar("§a⚔ PVP режим закончился!");
            }
        }
    }

    private void applyFlightSpeedModifier(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        UUID playerId = player.getUniqueId();
        boolean inPvp = isInPvpMode.getOrDefault(playerId, false);

        if (inPvp) {
            float normalSpeed = 0.1f;
            float pvpSpeed = normalSpeed * (float) getPvpFlightSpeedMultiplier();
            player.setFlySpeed(pvpSpeed);
        } else {
            player.setFlySpeed(0.1f);
        }
    }

    private void startFlight(Player player) {
        UUID playerId = player.getUniqueId();
        stopFlightTimer(player);

        int maxFlightTime = plugin.getConfigManager().getEleiFlightDuration();
        int cooldownPerSecond = plugin.getConfigManager().getEleiFlightCooldown();
        int fullCooldown = plugin.getConfigManager().getEleiFullCooldown();

        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        isFlying.put(playerId, true);
        usedFlightTime.put(playerId, 0);

        applyFlightSpeedModifier(player);
        startFlightParticles(player);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isElei(player) || !isFlying.getOrDefault(playerId, false)) {
                return;
            }

            updatePvpMode(player);

            int used = usedFlightTime.getOrDefault(playerId, 0);

            int drainAmount = 1;
            if (isInPvpMode(player)) {
                drainAmount = (int) Math.ceil(getPvpFlightDrainMultiplier());
            }

            used += drainAmount;
            usedFlightTime.put(playerId, used);

            int remaining = maxFlightTime - used;

            if (used >= maxFlightTime) {
                forceStopFlight(player);
                int cooldownSeconds = used * cooldownPerSecond;
                if (used >= maxFlightTime) {
                    cooldownSeconds = fullCooldown;
                }
                cooldownEndTime.put(playerId, System.currentTimeMillis() + cooldownSeconds * 1000L);
                featherCooldown.put(playerId, System.currentTimeMillis() + 5000L);
                player.sendActionBar("§c✈ Полёт закончился! Перезарядка пера 5с");
            } else {
                String status = isInPvpMode(player) ? "§c⚔ PVP режим! " : "";
                player.sendActionBar(status + "§e✈ §fПолёт: §a" + remaining + "§e сек");
            }
        }, 20L, 20L);

        flightTimerTasks.put(playerId, taskId);
    }

    private void stopFlightTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (flightTimerTasks.containsKey(playerId)) {
            Integer taskId = flightTimerTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            flightTimerTasks.remove(playerId);
        }
    }

    private void forceStopFlight(Player player) {
        UUID playerId = player.getUniqueId();
        stopFlightTimer(player);
        stopFlightParticles(player);

        int cooldownPerSecond = plugin.getConfigManager().getEleiFlightCooldown();
        int fullCooldown = plugin.getConfigManager().getEleiFullCooldown();
        int maxFlightTime = plugin.getConfigManager().getEleiFlightDuration();

        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(0.1f);
        }

        isFlying.put(playerId, false);

        int used = usedFlightTime.getOrDefault(playerId, 0);
        if (used > 0) {
            int cooldownSeconds = used * cooldownPerSecond;
            if (used >= maxFlightTime) {
                cooldownSeconds = fullCooldown;
            }
            cooldownEndTime.put(playerId, System.currentTimeMillis() + cooldownSeconds * 1000L);
        }

        // Дым при окончании полёта
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
    }

    private void startFlightParticles(Player player) {
        UUID playerId = player.getUniqueId();
        stopFlightParticles(player);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isElei(player) || !isFlying.getOrDefault(playerId, false)) {
                stopFlightParticles(player);
                return;
            }

            // Облачка и искры во время полёта
            player.getWorld().spawnParticle(Particle.CLOUD,
                    player.getLocation().subtract(0, 0.3, 0), 2, 0.2, 0.1, 0.2, 0.01);
            player.getWorld().spawnParticle(Particle.WAX_OFF,
                    player.getLocation().add(0, 0.5, 0), 1, 0.3, 0.3, 0.3, 0);
        }, 0L, 4L);

        flightParticleTasks.put(playerId, taskId);
    }

    private void stopFlightParticles(Player player) {
        UUID playerId = player.getUniqueId();
        if (flightParticleTasks.containsKey(playerId)) {
            Integer taskId = flightParticleTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            flightParticleTasks.remove(playerId);
        }
    }

    private void startActionBar(Player player) {
        UUID playerId = player.getUniqueId();
        stopActionBar(player);

        boolean showActionBar = plugin.getConfigManager().isEleiShowActionBar();
        if (!showActionBar) return;

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isElei(player)) {
                stopActionBar(player);
                return;
            }

            updatePvpMode(player);
            updateActionBar(player);

        }, 0L, 20L);

        actionBarTasks.put(playerId, taskId);
    }

    private void stopActionBar(Player player) {
        UUID playerId = player.getUniqueId();
        if (actionBarTasks.containsKey(playerId)) {
            Integer taskId = actionBarTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            actionBarTasks.remove(playerId);
        }
    }

    private void updateActionBar(Player player) {
        UUID playerId = player.getUniqueId();
        int maxFlightTime = plugin.getConfigManager().getEleiFlightDuration();

        boolean currentlyFlying = isFlying.getOrDefault(playerId, false);
        long currentTime = System.currentTimeMillis();
        boolean inPvp = isInPvpMode(player);

        if (currentlyFlying) {
            int used = usedFlightTime.getOrDefault(playerId, 0);
            int remaining = maxFlightTime - used;
            String pvpStatus = inPvp ? "§c⚔ " : "";
            player.sendActionBar(pvpStatus + "§e✈ §fПолёт: §a" + remaining + "§e сек");
        } else if (featherCooldown.containsKey(playerId)) {
            long remaining = featherCooldown.get(playerId) - currentTime;
            if (remaining > 0) {
                long seconds = (remaining / 1000) + 1;
                player.sendActionBar("§cПеро перезаряжается: " + seconds + "с");
            } else {
                featherCooldown.remove(playerId);
            }
        } else if (cooldownEndTime.containsKey(playerId)) {
            long cooldownEnd = cooldownEndTime.get(playerId);
            if (currentTime < cooldownEnd) {
                long remaining = (cooldownEnd - currentTime) / 1000;
                long minutes = remaining / 60;
                long seconds = remaining % 60;
                player.sendActionBar("§e✈ §fПерезарядка: §c" + minutes + ":" + String.format("%02d", seconds));
            } else {
                cooldownEndTime.remove(playerId);
            }
        } else if (inPvp) {
            Long cooldownEnd = pvpCooldownEndTime.get(playerId);
            if (cooldownEnd != null) {
                long remaining = (cooldownEnd - currentTime) / 1000;
                if (remaining > 0) {
                    player.sendActionBar("§c⚔ PVP режим: " + remaining + "с");
                }
            }
        }
    }

    private void startWindChargeTimer(Player player) {
        UUID playerId = player.getUniqueId();
        stopWindChargeTimer(player);

        int interval = plugin.getConfigManager().getEleiWindChargeInterval() * 20;
        int maxWindCharges = plugin.getConfigManager().getEleiMaxWindCharges();

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isElei(player)) {
                stopWindChargeTimer(player);
                return;
            }

            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            int windChargeCount = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && isWindCharge(item)) {
                    windChargeCount += item.getAmount();
                }
            }

            if (windChargeCount < maxWindCharges) {
                giveWindCharge(player);
            }

        }, interval, interval);

        windChargeTasks.put(playerId, taskId);
    }

    private void stopWindChargeTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (windChargeTasks.containsKey(playerId)) {
            Integer taskId = windChargeTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            windChargeTasks.remove(playerId);
        }
    }

    private ItemStack createWindCharge() {
        int maxWindCharges = plugin.getConfigManager().getEleiMaxWindCharges();
        int interval = plugin.getConfigManager().getEleiWindChargeInterval();

        ItemStack charge = new ItemStack(Material.WIND_CHARGE, 1);
        ItemMeta meta = charge.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(WIND_CHARGE_NAME);
            List<String> lore = new ArrayList<>();
            lore.add("§7Заряд ветра Элея");
            lore.add("§7Выпадает каждые " + interval + " секунд");
            lore.add("§7Максимум: " + maxWindCharges);
            meta.setLore(lore);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            charge.setItemMeta(meta);
        }

        return charge;
    }

    private boolean isWindCharge(ItemStack item) {
        if (item == null || item.getType() != Material.WIND_CHARGE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() &&
                meta.getDisplayName().equals(WIND_CHARGE_NAME);
    }

    private void giveWindCharge(Player player) {
        int maxWindCharges = plugin.getConfigManager().getEleiMaxWindCharges();

        int windChargeCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isWindCharge(item)) {
                windChargeCount += item.getAmount();
            }
        }

        if (windChargeCount >= maxWindCharges) {
            return;
        }

        ItemStack charge = createWindCharge();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(charge);

        if (!leftover.isEmpty()) {
            World world = player.getWorld();
            Location dropLoc = player.getLocation().add(0, 1, 0);
            world.dropItem(dropLoc, charge);
        }
    }

    private void removeAllWindCharges(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isWindCharge(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
    }

    private boolean isElei(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }

    private ItemStack createMagicFeather() {
        ItemStack feather = new ItemStack(Material.FEATHER, 1);
        ItemMeta meta = feather.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(FEATHER_NAME);
            List<String> lore = new ArrayList<>();
            lore.add("§7Используйте для активации полёта");
            lore.add("§7Правая кнопка мыши - вкл/выкл полёт");
            lore.add("§7После остановки полёта - перезарядка 5 сек");
            meta.setLore(lore);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            feather.setItemMeta(meta);
        }

        return feather;
    }

    private boolean isMagicFeather(ItemStack item) {
        if (item == null || item.getType() != Material.FEATHER) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(FEATHER_NAME);
    }

    private void giveMagicFeather(Player player) {
        UUID playerId = player.getUniqueId();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isMagicFeather(item)) {
                featherItems.put(playerId, item);
                return;
            }
        }

        ItemStack feather = createMagicFeather();
        featherItems.put(playerId, feather);

        int featherSlot = plugin.getConfigManager().getEleiFeatherSlot();
        if (featherSlot >= 0 && featherSlot < 9 && (player.getInventory().getItem(featherSlot) == null || player.getInventory().getItem(featherSlot).getType() == Material.AIR)) {
            player.getInventory().setItem(featherSlot, feather);
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack slotItem = player.getInventory().getItem(i);
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    player.getInventory().setItem(i, feather);
                    break;
                }
            }
        }
        player.updateInventory();
    }

    private void removeMagicFeather(Player player) {
        UUID playerId = player.getUniqueId();

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isMagicFeather(item)) {
                player.getInventory().setItem(i, null);
                break;
            }
        }

        featherItems.remove(playerId);
        player.updateInventory();
    }

    private void startSpeedEffect(Player player) {
        UUID playerId = player.getUniqueId();
        stopSpeedEffect(player);

        int speedLevel = plugin.getConfigManager().getEleiSpeedLevel();
        int amplifier = Math.max(0, speedLevel - 1);

        int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isElei(player)) {
                stopSpeedEffect(player);
                return;
            }

            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            player.removePotionEffect(PotionEffectType.SPEED);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    100,
                    amplifier,
                    false,
                    false
            ));

        }, 0L, 80L);

        speedTasks.put(playerId, taskId);
    }

    private void stopSpeedEffect(Player player) {
        UUID playerId = player.getUniqueId();
        if (speedTasks.containsKey(playerId)) {
            Integer taskId = speedTasks.get(playerId);
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
            speedTasks.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (isElei(attacker)) {
                activatePvpMode(attacker);
                if (isFlying.getOrDefault(attacker.getUniqueId(), false) && attacker.isFlying()) {
                    applyFlightSpeedModifier(attacker);
                }
            }
        }

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (isElei(victim)) {
                // Режим ПвП активируется только от удара игрока
                if (event.getDamager() instanceof Player) {
                    activatePvpMode(victim);
                    if (isFlying.getOrDefault(victim.getUniqueId(), false) && victim.isFlying()) {
                        applyFlightSpeedModifier(victim);
                    }
                }
                // Удары от мобов НЕ активируют ПвП-режим
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isElei(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyEleiEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isElei(player)) {
            removeEleiEffects(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isElei(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        if (!isMagicFeather(item)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getHand() != EquipmentSlot.HAND) return;

        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (featherCooldown.containsKey(playerId)) {
            long remaining = featherCooldown.get(playerId) - currentTime;
            if (remaining > 0) {
                long seconds = (remaining / 1000) + 1;
                player.sendActionBar("§cПеро перезаряжается: " + seconds + "с");
                return;
            } else {
                featherCooldown.remove(playerId);
            }
        }

        if (cooldownEndTime.containsKey(playerId)) {
            long cooldownEnd = cooldownEndTime.get(playerId);
            if (currentTime < cooldownEnd) {
                long remaining = (cooldownEnd - currentTime) / 1000;
                long minutes = remaining / 60;
                long seconds = remaining % 60;
                player.sendActionBar("§e✈ §fПерезарядка: §c" + minutes + ":" + String.format("%02d", seconds));
                return;
            } else {
                cooldownEndTime.remove(playerId);
            }
        }

        boolean currentlyFlying = isFlying.getOrDefault(playerId, false);

        if (currentlyFlying) {
            forceStopFlight(player);
            featherCooldown.put(playerId, currentTime + 5000L);
            player.sendActionBar("§cПеро перезаряжается 5 секунд");
        } else {
            startFlight(player);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
            player.sendActionBar("§a✈ Полёт активирован");
        }
        player.updateInventory();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!isElei(player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isElei(player)) {
            UUID playerId = player.getUniqueId();
            usedFlightTime.remove(playerId);
            cooldownEndTime.remove(playerId);
            featherCooldown.remove(playerId);
            isFlying.remove(playerId);
            pvpCooldownEndTime.remove(playerId);
            isInPvpMode.remove(playerId);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyEleiEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (isElei(player)) {
            UUID playerId = player.getUniqueId();
            usedFlightTime.remove(playerId);
            cooldownEndTime.remove(playerId);
            featherCooldown.remove(playerId);
            isFlying.remove(playerId);
            pvpCooldownEndTime.remove(playerId);
            isInPvpMode.remove(playerId);

            Iterator<ItemStack> iterator = event.getDrops().iterator();
            while (iterator.hasNext()) {
                ItemStack item = iterator.next();
                if (isMagicFeather(item) || isWindCharge(item)) {
                    iterator.remove();
                }
            }
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (!isElei(player)) return;

        UUID playerId = player.getUniqueId();
        GameMode newMode = event.getNewGameMode();

        if (newMode == GameMode.CREATIVE || newMode == GameMode.SPECTATOR) {
            player.setAllowFlight(true);
            player.setFlying(true);
            isFlying.put(playerId, true);
        } else {
            if (isFlying.getOrDefault(playerId, false)) {
                startFlight(player);
            } else {
                forceStopFlight(player);
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (isElei(player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyEleiEffects(player);
            }, 5L);
        }
    }
}
