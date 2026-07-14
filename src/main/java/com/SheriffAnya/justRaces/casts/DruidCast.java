package com.SheriffAnya.justRaces.casts;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class DruidCast implements Listener {

    private final JustRaces plugin;
    private final String castName = "Друид";
    private final Map<UUID, Integer> biomeCheckTasks = new HashMap<>();
    private final Map<UUID, Integer> grassCheckTasks = new HashMap<>();
    private final Map<UUID, List<Wolf>> summonedWolves = new HashMap<>();
    private final Map<UUID, BossBar> strengthBossBars = new HashMap<>();

    private static final String BONE_NAME = "§aКость призыва Друида";

    private final Map<UUID, Long> wolfCooldowns = new HashMap<>();
    private final Map<UUID, Integer> wolfDespawnTasks = new HashMap<>();
    private final Map<UUID, Integer> boneActionBarTasks = new HashMap<>();

    public DruidCast(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void applyDruidEffects(Player player) {
        if (!isDruid(player)) {
            removeDruidEffects(player);
            return;
        }

        setDruidSize(player);
        setDruidHealth(player);
        startBiomeCheck(player);
        startGrassCheck(player);
        giveSummonBone(player);
        startBoneActionBar(player);
        applyWorldEffects(player);
    }

    public void removeDruidEffects(Player player) {
        UUID playerId = player.getUniqueId();

        stopBiomeCheck(player);
        stopGrassCheck(player);
        restoreOriginalSize(player);
        restoreOriginalHealth(player);
        removeWolves(player);
        removeSummonBone(player);
        stopBoneActionBar(player);
        removeWorldEffects(player);

        wolfCooldowns.remove(playerId);

        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        if (strengthBossBars.containsKey(playerId)) {
            strengthBossBars.get(playerId).removeAll();
            strengthBossBars.remove(playerId);
        }
    }

    private void setDruidSize(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize(castName);
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось установить размер Друида: " + e.getMessage());
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

    private void setDruidHealth(Player player) {
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
            plugin.getLogger().warning("Не удалось установить здоровье Друида: " + e.getMessage());
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

    private void startBiomeCheck(Player player) {
        UUID playerId = player.getUniqueId();
        stopBiomeCheck(player);

        boolean strengthInForest = plugin.getConfigManager().isDruidStrengthInForest();
        boolean speedInForest = plugin.getConfigManager().isDruidSpeedInForest();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isDruid(player)) {
                stopBiomeCheck(player);
                return;
            }

            boolean inForestBiome = isInForestBiome(player);

            if (speedInForest) {
                player.removePotionEffect(PotionEffectType.SPEED);
                if (inForestBiome) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SPEED,
                            100,
                            0,
                            false,
                            false
                    ));
                }
            }

            if (strengthInForest) {
                player.removePotionEffect(PotionEffectType.STRENGTH);
                if (inForestBiome) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.STRENGTH,
                            100,
                            0,
                            false,
                            false
                    ));
                }
            }

            boolean showBossbar = plugin.getConfigManager().isDruidBossbarShowStrength();
            if (showBossbar) {
                if (inForestBiome && strengthInForest) {
                    showStrengthBossBar(player, true);
                } else {
                    showStrengthBossBar(player, false);
                }
            }

            if (inForestBiome) {
                // Зелёные искры в лесу
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        player.getLocation().add(0, 1, 0), 2, 0.4, 0.5, 0.4, 0);
            }

        }, 0L, 40L);

        biomeCheckTasks.put(playerId, taskId);
    }

    private void stopBiomeCheck(Player player) {
        UUID playerId = player.getUniqueId();
        if (biomeCheckTasks.containsKey(playerId)) {
            Integer taskId = biomeCheckTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            biomeCheckTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        showStrengthBossBar(player, false);
    }

    private void startGrassCheck(Player player) {
        UUID playerId = player.getUniqueId();
        stopGrassCheck(player);

        boolean regenerationOnGrass = plugin.getConfigManager().isDruidRegenerationOnGrass();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isDruid(player)) {
                stopGrassCheck(player);
                return;
            }

            boolean onGrass = isOnGrass(player);

            if (regenerationOnGrass) {
                player.removePotionEffect(PotionEffectType.REGENERATION);
                if (onGrass) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.REGENERATION,
                            100,
                            0,
                            false,
                            false
                    ));
                }
            }

            if (onGrass) {
                player.getWorld().spawnParticle(Particle.COMPOSTER,
                        player.getLocation().add(0, 0.1, 0), 2, 0.3, 0.05, 0.3, 0);
            }

        }, 0L, 40L);

        grassCheckTasks.put(playerId, taskId);
    }

    private void stopGrassCheck(Player player) {
        UUID playerId = player.getUniqueId();
        if (grassCheckTasks.containsKey(playerId)) {
            Integer taskId = grassCheckTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            grassCheckTasks.remove(playerId);
        }
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }

    private void summonWolves(Player player) {
        UUID playerId = player.getUniqueId();
        removeWolves(player);

        List<Wolf> wolves = new ArrayList<>();
        Location playerLoc = player.getLocation();

        int wolfCount = plugin.getConfigManager().getDruidWolfCount();

        for (int i = 0; i < wolfCount; i++) {
            Wolf wolf = player.getWorld().spawn(playerLoc, Wolf.class);
            wolf.setOwner(player);
            wolf.setTamed(true);
            wolf.setAdult();
            wolf.setCollarColor(DyeColor.GREEN);
            wolf.setCustomName("§aСпутник Друида");
            wolf.setCustomNameVisible(true);
            wolf.setSilent(true);

            wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, false, false));

            wolves.add(wolf);
        }

        summonedWolves.put(playerId, wolves);

        int durationSeconds = plugin.getConfigManager().getDruidWolfSummonDuration();

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, playerLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_WOLF_GROWL, 1.0f, 1.0f);
        player.sendMessage("§aВаши верные волки призваны на помощь! (" + durationSeconds + "с)");

        int despawnTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            removeWolves(player);
            if (player.isOnline()) {
                player.sendActionBar("§eВолки Друида рассеялись...");
            }
        }, durationSeconds * 20L);

        wolfDespawnTasks.put(playerId, despawnTaskId);
    }

    private void removeWolves(Player player) {
        UUID playerId = player.getUniqueId();
        if (summonedWolves.containsKey(playerId)) {
            for (Wolf wolf : summonedWolves.get(playerId)) {
                if (wolf != null && wolf.isValid()) {
                    wolf.remove();
                }
            }
            summonedWolves.remove(playerId);
        }

        if (wolfDespawnTasks.containsKey(playerId)) {
            Integer taskId = wolfDespawnTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            wolfDespawnTasks.remove(playerId);
        }
    }

    private void applyWorldEffects(Player player) {
        World world = player.getWorld();

        player.removePotionEffect(PotionEffectType.SLOWNESS);

        boolean netherSlowness = plugin.getConfigManager().isDruidNetherSlowness();

        if (netherSlowness && world.getEnvironment() == World.Environment.NETHER) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false
            ));
            player.sendMessage("§cАдская энергия замедляет вас...");
        }
    }

    private void removeWorldEffects(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private boolean isInForestBiome(Player player) {
        Biome biome = player.getLocation().getBlock().getBiome();

        return biome == Biome.FOREST ||
                biome == Biome.FLOWER_FOREST ||
                biome == Biome.BIRCH_FOREST ||
                biome == Biome.DARK_FOREST ||
                biome == Biome.OLD_GROWTH_BIRCH_FOREST ||
                biome == Biome.OLD_GROWTH_SPRUCE_TAIGA ||
                biome == Biome.TAIGA ||
                biome == Biome.SNOWY_TAIGA ||
                biome.getKey().getKey().toUpperCase().contains("FOREST") ||
                biome.getKey().getKey().toUpperCase().contains("TAIGA");
    }

    private boolean isOnGrass(Player player) {
        Location loc = player.getLocation();
        Material blockType = loc.getBlock().getType();

        return blockType == Material.GRASS_BLOCK ||
                blockType == Material.PODZOL ||
                blockType == Material.MYCELIUM ||
                blockType == Material.MOSS_BLOCK;
    }

    private void showStrengthBossBar(Player player, boolean show) {
        UUID playerId = player.getUniqueId();

        boolean showBossbar = plugin.getConfigManager().isDruidBossbarShowStrength();

        if (!showBossbar) return;

        if (show) {
            if (!strengthBossBars.containsKey(playerId)) {
                BossBar bossBar = Bukkit.createBossBar(
                        "§aСила Друида в лесу",
                        BarColor.GREEN,
                        BarStyle.SOLID
                );
                bossBar.setProgress(1.0);
                bossBar.addPlayer(player);
                strengthBossBars.put(playerId, bossBar);
            }
        } else {
            if (strengthBossBars.containsKey(playerId)) {
                strengthBossBars.get(playerId).removeAll();
                strengthBossBars.remove(playerId);
            }
        }
    }

    private ItemStack createSummonBone() {
        ItemStack bone = new ItemStack(Material.BONE, 1);
        ItemMeta meta = bone.getItemMeta();

        if (meta != null) {
            int durationSeconds = plugin.getConfigManager().getDruidWolfSummonDuration();
            int cooldownSeconds = plugin.getConfigManager().getDruidWolfSummonCooldown();

            meta.setDisplayName(BONE_NAME);
            meta.setLore(Arrays.asList(
                    "§7Особый предмет Друида",
                    "§7ПКМ: призвать волков-спутников",
                    "§7Волки служат " + durationSeconds + " секунд",
                    "§7Перезарядка: " + cooldownSeconds + " секунд"
            ));
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            bone.setItemMeta(meta);
        }

        return bone;
    }

    private boolean isSummonBone(ItemStack item) {
        if (item == null || item.getType() != Material.BONE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() &&
                meta.getDisplayName().equals(BONE_NAME);
    }

    private void giveSummonBone(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSummonBone(item)) {
                return;
            }
        }

        ItemStack bone = createSummonBone();

        boolean placed = false;
        for (int i = 0; i < 9; i++) {
            ItemStack slotItem = player.getInventory().getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                player.getInventory().setItem(i, bone);
                placed = true;
                break;
            }
        }

        if (!placed) {
            player.getInventory().addItem(bone);
        }

        player.updateInventory();
    }

    private void removeSummonBone(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isSummonBone(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
    }

    private void startBoneActionBar(Player player) {
        UUID playerId = player.getUniqueId();
        stopBoneActionBar(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || !isDruid(player)) {
                stopBoneActionBar(player);
                return;
            }

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (!isSummonBone(mainHand) && !isSummonBone(offHand)) return;

            long remaining = getWolfCooldownRemaining(player);
            if (remaining > 0) {
                long seconds = (remaining / 1000) + 1;
                player.sendActionBar("§cПризыв волков перезаряжается: " + seconds + "с");
            } else {
                player.sendActionBar("§aКость готова к использованию!");
            }
        }, 0L, 10L);

        boneActionBarTasks.put(playerId, taskId);
    }

    private void stopBoneActionBar(Player player) {
        UUID playerId = player.getUniqueId();
        if (boneActionBarTasks.containsKey(playerId)) {
            Integer taskId = boneActionBarTasks.get(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            boneActionBarTasks.remove(playerId);
        }
    }

    private long getWolfCooldownRemaining(Player player) {
        UUID playerId = player.getUniqueId();
        if (!wolfCooldowns.containsKey(playerId)) return 0L;
        long remaining = wolfCooldowns.get(playerId) - System.currentTimeMillis();
        return Math.max(remaining, 0L);
    }

    @EventHandler
    public void onPlayerInteractBone(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isDruid(player)) return;

        ItemStack item = event.getItem();
        if (item == null || !isSummonBone(item)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        long remaining = getWolfCooldownRemaining(player);

        if (remaining > 0) {
            long seconds = (remaining / 1000) + 1;
            player.sendActionBar("§cПризыв волков перезаряжается: " + seconds + "с");
            return;
        }

        wolfCooldowns.put(playerId, System.currentTimeMillis() + (plugin.getConfigManager().getDruidWolfSummonCooldown() * 1000L));
        summonWolves(player);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isDruid(player)) return;

        if (event.getCause() == DamageCause.FIRE ||
                event.getCause() == DamageCause.FIRE_TICK ||
                event.getCause() == DamageCause.LAVA ||
                event.getCause() == DamageCause.HOT_FLOOR) {

            boolean hasFireResistance = player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);

            if (!hasFireResistance) {
                double fireMultiplier = plugin.getConfigManager().getDruidFireDamageMultiplier();
                event.setDamage(event.getDamage() * fireMultiplier);
                player.sendMessage("§cОгонь наносит вам повышенный урон! Используйте огнеупорность!");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isDruid(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyDruidEffects(player);
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isDruid(player)) {
            removeDruidEffects(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isDruid(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyDruidEffects(player);
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!isDruid(player)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyWorldEffects(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isDruid(player)) return;

        removeWolves(player);

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isSummonBone(item)) {
                iterator.remove();
            }
        }
    }

    private boolean isDruid(Player player) {
        String cast = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName.equals(cast);
    }
}
