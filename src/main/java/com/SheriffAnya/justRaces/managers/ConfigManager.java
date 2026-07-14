package com.SheriffAnya.justRaces.managers;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Set;

public class ConfigManager {

    private final JustRaces plugin;
    private FileConfiguration config;

    public ConfigManager(JustRaces plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        if (config == null) {
            plugin.getLogger().severe("Конфигурация не загружена!");
            return;
        }

        setupDefaultConfig();
        plugin.getLogger().info("Конфигурация загружена успешно");
    }

    private void setupDefaultConfig() {
        boolean changed = false;

        if (!config.contains("chat")) {
            config.set("chat.enabled", false);
            config.set("chat.format", "{cast_prefix} &r{player}: &7{message}");
            config.set("chat.global_format", "{cast_prefix} &r{player}: &7{message}");
            changed = true;
        }

        if (!config.contains("casts")) {
            createDefaultCastsSection();
            changed = true;
        }

        if (!config.contains("rusal-settings")) {
            createDefaultRusalSettings();
            changed = true;
        }

        if (!config.contains("undead-settings")) {
            createDefaultUndeadSettings();
            changed = true;
        }

        if (!config.contains("elei-settings")) {
            createDefaultEleiSettings();
            changed = true;
        }

        if (!config.contains("ender-settings")) {
            createDefaultEnderSettings();
            changed = true;
        }

        if (!config.contains("giant-settings")) {
            createDefaultGiantSettings();
            changed = true;
        }

        if (!config.contains("fortune-settings")) {
            createDefaultFortuneSettings();
            changed = true;
        }

        if (!config.contains("golem-settings")) {
            createDefaultGolemSettings();
            changed = true;
        }

        if (!config.contains("warden-settings")) {
            createDefaultWardenSettings();
            changed = true;
        }

        if (!config.contains("druid-settings")) {
            createDefaultDruidSettings();
            changed = true;
        }

        if (!config.contains("runekeeper-settings")) {
            createDefaultRuneKeeperSettings();
            changed = true;
        }

        if (!config.contains("respawn")) {
            createDefaultRespawnSection();
            changed = true;
        }

        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("Создана конфигурация по умолчанию");
        }
    }

    private void createDefaultCastsSection() {
        config.set("casts.Русал.health", 20.0);
        config.set("casts.Русал.size", 1.0);
        config.set("casts.Русал.can-breathe-underwater", true);
        config.set("casts.Русал.needs-water-to-breathe", true);
        config.set("casts.Русал.underwater-mining-speed", 1.5);
        config.set("casts.Русал.max-size", 4);
        config.set("casts.Русал.color", "&b");

        config.set("casts.Элей.health", 14.0);
        config.set("casts.Элей.size", 0.55);
        config.set("casts.Элей.max-size", 4);
        config.set("casts.Элей.color", "&a");

        config.set("casts.Фортуна.health", 15.0);
        config.set("casts.Фортуна.size", 1.0);
        config.set("casts.Фортуна.max-size", 4);
        config.set("casts.Фортуна.color", "&6");

        config.set("casts.Нежить.health", 17.0);
        config.set("casts.Нежить.size", 1.0);
        config.set("casts.Нежить.max-size", 4);
        config.set("casts.Нежить.color", "&8");

        config.set("casts.Голем.health", 25.0);
        config.set("casts.Голем.size", 1.5);
        config.set("casts.Голем.max-size", 4);
        config.set("casts.Голем.color", "&7");

        config.set("casts.Варден.health", 50.0);
        config.set("casts.Варден.size", 2.0);
        config.set("casts.Варден.max-size", 1);
        config.set("casts.Варден.color", "&5");

        config.set("casts.Эндермен.health", 24.0);
        config.set("casts.Эндермен.size", 1.5);
        config.set("casts.Эндермен.max-size", 4);
        config.set("casts.Эндермен.color", "&d");

        config.set("casts.Гигант.health", 35.0);
        config.set("casts.Гигант.size", 1.625);
        config.set("casts.Гигант.max-size", 4);
        config.set("casts.Гигант.color", "&4");

        config.set("casts.Тень.health", 15.0);
        config.set("casts.Тень.size", 1.0);
        config.set("casts.Тень.max-size", 4);
        config.set("casts.Тень.color", "&0");

        config.set("casts.Друид.health", 23.0);
        config.set("casts.Друид.size", 1.0);
        config.set("casts.Друид.max-size", 4);
        config.set("casts.Друид.color", "&2");

        config.set("casts.Эфрит.health", 19.0);
        config.set("casts.Эфрит.size", 1.0);
        config.set("casts.Эфрит.max-size", 4);
        config.set("casts.Эфрит.color", "&c");

        config.set("casts.ХранительРун.health", 7.0);
        config.set("casts.ХранительРун.size", 1.25);
        config.set("casts.ХранительРун.max-size", 1);
        config.set("casts.ХранительРун.color", "&5");
    }

    private void createDefaultRusalSettings() {
        config.set("rusal-settings.drowning-damage", 4.0);
        config.set("rusal-settings.air-depletion-rate", 30);
        config.set("rusal-settings.dolphin-grace-level", 2);
        config.set("rusal-settings.fish-food-restore", 5);
        config.set("rusal-settings.night-vision-in-water", true);
        config.set("rusal-settings.conduit-breathing-range", 25);
    }

    private void createDefaultUndeadSettings() {
        config.set("undead-settings.sun-damage", 2.0);
        config.set("undead-settings.healing-potion-multiplier", 2.0);
        config.set("undead-settings.rotten-flesh-saturation", 5);
        config.set("undead-settings.blindness-in-light", true);
        config.set("undead-settings.night-vision-in-darkness", true);
    }

    private void createDefaultEleiSettings() {
        config.set("elei-settings.flight-duration", 40);
        config.set("elei-settings.flight-cooldown", 7);
        config.set("elei-settings.full-cooldown", 280);
        config.set("elei-settings.speed-level", 1);
        config.set("elei-settings.show-actionbar", true);
        config.set("elei-settings.wind-charge-interval", 20);
        config.set("elei-settings.max-wind-charges", 64);
        config.set("elei-settings.feather-slot", 0);
        config.set("elei-settings.pvp-cooldown", 15);
        config.set("elei-settings.pvp-flight-drain-multiplier", 2.0);
        config.set("elei-settings.pvp-flight-speed-multiplier", 0.5);
    }

    private void createDefaultEnderSettings() {
        config.set("ender-settings.attack-range", 4.5);
        config.set("ender-settings.water-damage", 2.5);
        config.set("ender-settings.rain-damage", 1.0);
        config.set("ender-settings.pearl-interval", 20);
        config.set("ender-settings.max-pearls", 16);
        config.set("ender-settings.endermite-spawn-chance", 80);
        config.set("ender-settings.chorus-fruit-bonus", true);
    }

    private void createDefaultGiantSettings() {
        config.set("giant-settings.fist-damage", 7.0);
        config.set("giant-settings.knockback-power", 1.5);
        config.set("giant-settings.mining-speed-boost", 1.2);
        config.set("giant-settings.movement-slowdown", 0.15);
        config.set("giant-settings.permanent-slowness", true);
        config.set("giant-settings.no-bed-sleeping", true);
    }

    private void createDefaultFortuneSettings() {
        config.set("fortune-settings.permanent-luck", true);
        config.set("fortune-settings.speed-duration", 60);
        config.set("fortune-settings.speed-level", 0);
        config.set("fortune-settings.damage-increase", 1.15);
        config.set("fortune-settings.totem-chance", 30);
        config.set("fortune-settings.blindness-on-totem", true);
        config.set("fortune-settings.blindness-duration", 200);
    }

    private void createDefaultGolemSettings() {
        config.set("golem-settings.damage-resistance", 0.15);
        config.set("golem-settings.knockback-power", 2.0);
        config.set("golem-settings.permanent-slowness", true);
        config.set("golem-settings.villager-discount", 0.5);
        config.set("golem-settings.iron-armor-unbreakable", true);
        config.set("golem-settings.no-knockback", true);
        config.set("golem-settings.knockback-attacker", true);
    }

    private void createDefaultWardenSettings() {
        config.set("warden-settings.sculk-bonus-range", 16);
        config.set("warden-settings.damage-bonus", 0.27);
        config.set("warden-settings.player-vision-range", 17);
        config.set("warden-settings.stun-chance", 5);
        config.set("warden-settings.stun-duration", 20);
        config.set("warden-settings.permanent-resistance", 2);
        config.set("warden-settings.no-armor", true);
        config.set("warden-settings.spawn-sculk-on-kill", true);
        config.set("warden-settings.leave-sculk-on-death", true);
        config.set("warden-settings.ignore-by-wardens", true);
    }

    private void createDefaultDruidSettings() {
        config.set("druid-settings.forest-bonus", 0.2);
        config.set("druid-settings.regeneration-on-grass", true);
        config.set("druid-settings.fire-damage-multiplier", 1.5);
        config.set("druid-settings.nether-slowness", true);
        config.set("druid-settings.wolf-count", 2);
        config.set("druid-settings.strength-in-forest", true);
        config.set("druid-settings.speed-in-forest", true);
        config.set("druid-settings.bossbar-show-strength", true);
        config.set("druid-settings.wolf-summon-duration", 90);
        config.set("druid-settings.wolf-summon-cooldown", 120);
    }

    private void createDefaultRuneKeeperSettings() {
        config.set("runekeeper-settings.rune-regen-interval", 30);
        config.set("runekeeper-settings.max-runes", 3);
        config.set("runekeeper-settings.crit-multiplier", 1.5);
        config.set("runekeeper-settings.exp-per-hit", 1);
        config.set("runekeeper-settings.exp-per-damage", 2);
        config.set("runekeeper-settings.night-vision-with-runes", true);
        config.set("runekeeper-settings.no-shields", true);
        config.set("runekeeper-settings.no-totems", true);
        config.set("runekeeper-settings.show-bossbar", true);

        config.set("runekeeper-settings.stages.0.exp", 5);
        config.set("runekeeper-settings.stages.0.health", 7.0);
        config.set("runekeeper-settings.stages.0.speed", 0);
        config.set("runekeeper-settings.stages.0.jump", 0.75);
        config.set("runekeeper-settings.stages.0.size", 1.25);

        config.set("runekeeper-settings.stages.1.exp", 15);
        config.set("runekeeper-settings.stages.1.health", 12.0);
        config.set("runekeeper-settings.stages.1.speed", 0);
        config.set("runekeeper-settings.stages.1.jump", 1.0);
        config.set("runekeeper-settings.stages.1.size", 1.4);

        config.set("runekeeper-settings.stages.2.exp", 25);
        config.set("runekeeper-settings.stages.2.health", 17.0);
        config.set("runekeeper-settings.stages.2.speed", 1);
        config.set("runekeeper-settings.stages.2.jump", 1.25);
        config.set("runekeeper-settings.stages.2.size", 1.55);

        config.set("runekeeper-settings.stages.3.exp", 50);
        config.set("runekeeper-settings.stages.3.health", 22.0);
        config.set("runekeeper-settings.stages.3.speed", 1);
        config.set("runekeeper-settings.stages.3.jump", 1.5);
        config.set("runekeeper-settings.stages.3.size", 1.7);

        config.set("runekeeper-settings.stages.4.exp", 75);
        config.set("runekeeper-settings.stages.4.health", 27.0);
        config.set("runekeeper-settings.stages.4.speed", 2);
        config.set("runekeeper-settings.stages.4.jump", 1.75);
        config.set("runekeeper-settings.stages.4.size", 1.85);

        config.set("runekeeper-settings.stages.5.exp", 125);
        config.set("runekeeper-settings.stages.5.health", 32.0);
        config.set("runekeeper-settings.stages.5.speed", 2);
        config.set("runekeeper-settings.stages.5.jump", 2.0);
        config.set("runekeeper-settings.stages.5.size", 2.0);
    }

    private void createDefaultRespawnSection() {
        config.set("respawn.Русал.world", "world");
        config.set("respawn.Русал.x", -354.0);
        config.set("respawn.Русал.y", 61.0);
        config.set("respawn.Русал.z", -12.0);
        config.set("respawn.Русал.radius", 20);

        config.set("respawn.Элей.world", "world");
        config.set("respawn.Элей.x", 211.0);
        config.set("respawn.Элей.y", 72.0);
        config.set("respawn.Элей.z", 290.0);
        config.set("respawn.Элей.radius", 79);

        config.set("respawn.Фортуна.world", "world");
        config.set("respawn.Фортуна.x", -603.0);
        config.set("respawn.Фортуна.y", 104.0);
        config.set("respawn.Фортуна.z", 11.0);
        config.set("respawn.Фортуна.radius", 77);

        config.set("respawn.Нежить.world", "world");
        config.set("respawn.Нежить.x", -225.0);
        config.set("respawn.Нежить.y", 78.0);
        config.set("respawn.Нежить.z", 148.0);
        config.set("respawn.Нежить.radius", 35);

        config.set("respawn.Голем.world", "world");
        config.set("respawn.Голем.x", -192.0);
        config.set("respawn.Голем.y", 87.0);
        config.set("respawn.Голем.z", 250.0);
        config.set("respawn.Голем.radius", 25);

        config.set("respawn.Варден.world", "world");
        config.set("respawn.Варден.x", 287.0);
        config.set("respawn.Варден.y", 67.0);
        config.set("respawn.Варден.z", 163.0);
        config.set("respawn.Варден.radius", 25);

        config.set("respawn.Эндермен.world", "world");
        config.set("respawn.Эндермен.x", 344.0);
        config.set("respawn.Эндермен.y", 118.0);
        config.set("respawn.Эндермен.z", 192.0);
        config.set("respawn.Эндермен.radius", 77);

        config.set("respawn.Гигант.world", "world");
        config.set("respawn.Гигант.x", -35.0);
        config.set("respawn.Гигант.y", 96.0);
        config.set("respawn.Гигант.z", 445.0);
        config.set("respawn.Гигант.radius", 50);

        config.set("respawn.Тень.world", "world");
        config.set("respawn.Тень.x", -110.0);
        config.set("respawn.Тень.y", 86.0);
        config.set("respawn.Тень.z", -80.0);
        config.set("respawn.Тень.radius", 70);

        config.set("respawn.Друид.world", "world");
        config.set("respawn.Друид.x", -143.0);
        config.set("respawn.Друид.y", 72.0);
        config.set("respawn.Друид.z", -171.0);
        config.set("respawn.Друид.radius", 77);

        config.set("respawn.Эфрит.world", "world_nether");
        config.set("respawn.Эфрит.x", -20.0);
        config.set("respawn.Эфрит.y", 65.0);
        config.set("respawn.Эфрит.z", 59.0);
        config.set("respawn.Эфрит.radius", 10);

        config.set("respawn.ХранительРун.world", "world");
        config.set("respawn.ХранительРун.x", -500.0);
        config.set("respawn.ХранительРун.y", 74.0);
        config.set("respawn.ХранительРун.z", -500.0);
        config.set("respawn.ХранительРун.radius", 20);
    }

    public void saveConfig() {
        if (config != null) {
            plugin.saveConfig();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // ==================== ОСНОВНЫЕ ГЕТТЕРЫ ДЛЯ РАС ====================

    public double getCastHealth(String castName) {
        if (config == null) return 20.0;
        String path = "casts." + castName + ".health";
        if (config.contains(path)) {
            return config.getDouble(path, 20.0);
        }
        switch (castName) {
            case "Русал": return 20.0;
            case "Элей": return 14.0;
            case "Фортуна": return 15.0;
            case "Нежить": return 17.0;
            case "Голем": return 25.0;
            case "Варден": return 50.0;
            case "Эндермен": return 24.0;
            case "Гигант": return 35.0;
            case "Тень": return 15.0;
            case "Друид": return 23.0;
            case "Эфрит": return 19.0;
            case "ХранительРун": return 7.0;
            default: return 20.0;
        }
    }

    public float getCastSize(String castName) {
        if (config == null) return 1.0f;
        String path = "casts." + castName + ".size";
        if (config.contains(path)) {
            return (float) config.getDouble(path, 1.0);
        }
        switch (castName) {
            case "Элей": return 0.55f;
            case "Голем": return 1.5f;
            case "Варден": return 2.0f;
            case "Эндермен": return 1.5f;
            case "Гигант": return 1.625f;
            case "ХранительРун": return 1.25f;
            default: return 1.0f;
        }
    }

    public int getCastMaxSize(String castName) {
        if (config == null) return (castName.equals("Варден") || castName.equals("ХранительРун")) ? 1 : 4;
        String path = "casts." + castName + ".max-size";
        if (config.contains(path)) {
            return config.getInt(path, (castName.equals("Варден") || castName.equals("ХранительРун")) ? 1 : 4);
        }
        return (castName.equals("Варден") || castName.equals("ХранительРун")) ? 1 : 4;
    }

    public String getCastColor(String castName) {
        if (config == null) return "&f";
        String path = "casts." + castName + ".color";
        if (config.contains(path)) {
            return config.getString(path, "&f");
        }
        switch (castName) {
            case "Русал": return "&b";
            case "Элей": return "&a";
            case "Фортуна": return "&6";
            case "Нежить": return "&8";
            case "Голем": return "&7";
            case "Варден": return "&5";
            case "Эндермен": return "&d";
            case "Гигант": return "&4";
            case "Тень": return "&0";
            case "Друид": return "&2";
            case "Эфрит": return "&c";
            case "ХранительРун": return "&5";
            default: return "&f";
        }
    }

    public boolean canBreatheUnderwater(String castName) {
        if (config == null) return false;
        String path = "casts." + castName + ".can-breathe-underwater";
        if (config.contains(path)) {
            return config.getBoolean(path, false);
        }
        return castName.equals("Русал");
    }

    public boolean needsWaterToBreathe(String castName) {
        if (config == null) return false;
        String path = "casts." + castName + ".needs-water-to-breathe";
        if (config.contains(path)) {
            return config.getBoolean(path, false);
        }
        return castName.equals("Русал");
    }

    public float getUnderwaterMiningSpeed(String castName) {
        if (config == null) return 1.0f;
        String path = "casts." + castName + ".underwater-mining-speed";
        if (config.contains(path)) {
            return (float) config.getDouble(path, 1.0);
        }
        return castName.equals("Русал") ? 1.5f : 1.0f;
    }

    public String[] getCastNames() {
        if (config == null || !config.contains("casts") || !config.isConfigurationSection("casts")) {
            return new String[]{
                    "Русал", "Элей", "Фортуна", "Нежить", "Голем", "Варден",
                    "Эндермен", "Гигант", "Тень", "Друид", "Эфрит", "ХранительРун"
            };
        }

        ConfigurationSection section = config.getConfigurationSection("casts");
        if (section != null) {
            Set<String> keys = section.getKeys(false);
            return keys.toArray(new String[0]);
        }

        return new String[]{
                "Русал", "Элей", "Фортуна", "Нежить", "Голем", "Варден",
                "Эндермен", "Гигант", "Тень", "Друид", "Эфрит", "ХранительРун"
        };
    }

    public boolean isSoloCast(String castName) {
        return getCastMaxSize(castName) == 1;
    }

    // ==================== Elei (Элей) ====================
    public int getEleiFlightDuration() { return config != null ? config.getInt("elei-settings.flight-duration", 40) : 40; }
    public int getEleiFlightCooldown() { return config != null ? config.getInt("elei-settings.flight-cooldown", 7) : 7; }
    public int getEleiFullCooldown() { return config != null ? config.getInt("elei-settings.full-cooldown", 280) : 280; }
    public int getEleiMaxWindCharges() { return config != null ? config.getInt("elei-settings.max-wind-charges", 64) : 64; }
    public int getEleiWindChargeInterval() { return config != null ? config.getInt("elei-settings.wind-charge-interval", 20) : 20; }
    public int getEleiFeatherSlot() { return config != null ? config.getInt("elei-settings.feather-slot", 0) : 0; }
    public int getEleiSpeedLevel() { return config != null ? config.getInt("elei-settings.speed-level", 1) : 1; }
    public boolean isEleiShowActionBar() { return config != null ? config.getBoolean("elei-settings.show-actionbar", true) : true; }
    public int getEleiPvpCooldown() { return config != null ? config.getInt("elei-settings.pvp-cooldown", 15) : 15; }
    public double getEleiPvpFlightDrainMultiplier() { return config != null ? config.getDouble("elei-settings.pvp-flight-drain-multiplier", 2.0) : 2.0; }
    public double getEleiPvpFlightSpeedMultiplier() { return config != null ? config.getDouble("elei-settings.pvp-flight-speed-multiplier", 0.5) : 0.5; }

    // ==================== Rusal (Русал) ====================
    public double getRusalDrowningDamage() { return config != null ? config.getDouble("rusal-settings.drowning-damage", 4.0) : 4.0; }
    public int getRusalAirDepletionRate() { return config != null ? config.getInt("rusal-settings.air-depletion-rate", 30) : 30; }
    public int getRusalDolphinGraceLevel() { return config != null ? config.getInt("rusal-settings.dolphin-grace-level", 2) : 2; }
    public int getRusalFishFoodRestore() { return config != null ? config.getInt("rusal-settings.fish-food-restore", 5) : 5; }
    public boolean isRusalNightVisionInWater() { return config != null ? config.getBoolean("rusal-settings.night-vision-in-water", true) : true; }
    public int getRusalConduitBreathingRange() { return config != null ? config.getInt("rusal-settings.conduit-breathing-range", 25) : 25; }

    // ==================== Undead (Нежить) ====================
    public double getUndeadSunDamage() { return config != null ? config.getDouble("undead-settings.sun-damage", 2.0) : 2.0; }
    public double getUndeadHealingMultiplier() { return config != null ? config.getDouble("undead-settings.healing-potion-multiplier", 2.0) : 2.0; }
    public int getUndeadRottenFleshSaturation() { return config != null ? config.getInt("undead-settings.rotten-flesh-saturation", 5) : 5; }
    public boolean isUndeadBlindnessInLight() { return config != null ? config.getBoolean("undead-settings.blindness-in-light", true) : true; }
    public boolean isUndeadNightVisionInDarkness() { return config != null ? config.getBoolean("undead-settings.night-vision-in-darkness", true) : true; }

    // ==================== Ender (Эндермен) ====================
    public double getEnderAttackRange() { return config != null ? config.getDouble("ender-settings.attack-range", 4.5) : 4.5; }
    public double getEnderWaterDamage() { return config != null ? config.getDouble("ender-settings.water-damage", 2.5) : 2.5; }
    public double getEnderRainDamage() { return config != null ? config.getDouble("ender-settings.rain-damage", 1.0) : 1.0; }
    public int getEnderPearlInterval() { return config != null ? config.getInt("ender-settings.pearl-interval", 20) : 20; }
    public int getEnderMaxPearls() { return config != null ? config.getInt("ender-settings.max-pearls", 16) : 16; }
    public int getEnderEndermiteSpawnChance() { return config != null ? config.getInt("ender-settings.endermite-spawn-chance", 80) : 80; }
    public boolean isEnderChorusFruitBonus() { return config != null ? config.getBoolean("ender-settings.chorus-fruit-bonus", true) : true; }

    // ==================== Giant (Гигант) ====================
    public double getGiantFistDamage() { return config != null ? config.getDouble("giant-settings.fist-damage", 7.0) : 7.0; }
    public double getGiantKnockbackPower() { return config != null ? config.getDouble("giant-settings.knockback-power", 1.5) : 1.5; }
    public double getGiantMiningSpeedBoost() { return config != null ? config.getDouble("giant-settings.mining-speed-boost", 1.2) : 1.2; }
    public boolean isGiantPermanentSlowness() { return config != null ? config.getBoolean("giant-settings.permanent-slowness", true) : true; }
    public boolean isGiantNoBedSleeping() { return config != null ? config.getBoolean("giant-settings.no-bed-sleeping", true) : true; }

    // ==================== Fortune (Фортуна) ====================
    public double getFortuneDamageIncrease() { return config != null ? config.getDouble("fortune-settings.damage-increase", 1.15) : 1.15; }
    public int getFortuneTotemChance() { return config != null ? config.getInt("fortune-settings.totem-chance", 30) : 30; }
    public int getFortuneSpeedDuration() { return config != null ? config.getInt("fortune-settings.speed-duration", 60) : 60; }
    public int getFortuneSpeedLevel() { return config != null ? config.getInt("fortune-settings.speed-level", 0) : 0; }
    public boolean isFortunePermanentLuck() { return config != null ? config.getBoolean("fortune-settings.permanent-luck", true) : true; }
    public boolean isFortuneBlindnessOnTotem() { return config != null ? config.getBoolean("fortune-settings.blindness-on-totem", true) : true; }
    public int getFortuneBlindnessDuration() { return config != null ? config.getInt("fortune-settings.blindness-duration", 200) : 200; }

    // ==================== Golem (Голем) ====================
    public double getGolemDamageResistance() { return config != null ? config.getDouble("golem-settings.damage-resistance", 0.15) : 0.15; }
    public double getGolemKnockbackPower() { return config != null ? config.getDouble("golem-settings.knockback-power", 2.0) : 2.0; }
    public double getGolemVillagerDiscount() { return config != null ? config.getDouble("golem-settings.villager-discount", 0.5) : 0.5; }
    public boolean isGolemIronArmorUnbreakable() { return config != null ? config.getBoolean("golem-settings.iron-armor-unbreakable", true) : true; }
    public boolean isGolemNoKnockback() { return config != null ? config.getBoolean("golem-settings.no-knockback", true) : true; }
    public boolean isGolemKnockbackAttacker() { return config != null ? config.getBoolean("golem-settings.knockback-attacker", true) : true; }

    // ==================== Warden (Варден) ====================
    public int getWardenSculkBonusRange() { return config != null ? config.getInt("warden-settings.sculk-bonus-range", 16) : 16; }
    public double getWardenDamageBonus() { return config != null ? config.getDouble("warden-settings.damage-bonus", 0.27) : 0.27; }
    public int getWardenPlayerVisionRange() { return config != null ? config.getInt("warden-settings.player-vision-range", 17) : 17; }
    public int getWardenStunChance() { return config != null ? config.getInt("warden-settings.stun-chance", 5) : 5; }
    public int getWardenStunDuration() { return config != null ? config.getInt("warden-settings.stun-duration", 20) : 20; }
    public int getWardenPermanentResistance() { return config != null ? config.getInt("warden-settings.permanent-resistance", 2) : 2; }
    public boolean isWardenNoArmor() { return config != null ? config.getBoolean("warden-settings.no-armor", true) : true; }
    public boolean isWardenSpawnSculkOnKill() { return config != null ? config.getBoolean("warden-settings.spawn-sculk-on-kill", true) : true; }
    public boolean isWardenLeaveSculkOnDeath() { return config != null ? config.getBoolean("warden-settings.leave-sculk-on-death", true) : true; }
    public boolean isWardenIgnoreByWardens() { return config != null ? config.getBoolean("warden-settings.ignore-by-wardens", true) : true; }

    // ==================== Druid (Друид) ====================
    public double getDruidForestBonus() { return config != null ? config.getDouble("druid-settings.forest-bonus", 0.2) : 0.2; }
    public boolean isDruidRegenerationOnGrass() { return config != null ? config.getBoolean("druid-settings.regeneration-on-grass", true) : true; }
    public double getDruidFireDamageMultiplier() { return config != null ? config.getDouble("druid-settings.fire-damage-multiplier", 1.5) : 1.5; }
    public boolean isDruidNetherSlowness() { return config != null ? config.getBoolean("druid-settings.nether-slowness", true) : true; }
    public int getDruidWolfCount() { return config != null ? config.getInt("druid-settings.wolf-count", 2) : 2; }
    public boolean isDruidStrengthInForest() { return config != null ? config.getBoolean("druid-settings.strength-in-forest", true) : true; }
    public boolean isDruidSpeedInForest() { return config != null ? config.getBoolean("druid-settings.speed-in-forest", true) : true; }
    public boolean isDruidBossbarShowStrength() { return config != null ? config.getBoolean("druid-settings.bossbar-show-strength", true) : true; }
    public int getDruidWolfSummonDuration() { return config != null ? config.getInt("druid-settings.wolf-summon-duration", 90) : 90; }
    public int getDruidWolfSummonCooldown() { return config != null ? config.getInt("druid-settings.wolf-summon-cooldown", 120) : 120; }

    // ==================== RuneKeeper (ХранительРун) ====================
    public int getRuneKeeperMaxRunes() { return config != null ? config.getInt("runekeeper-settings.max-runes", 3) : 3; }
    public int getRuneKeeperRuneRegenInterval() { return config != null ? config.getInt("runekeeper-settings.rune-regen-interval", 30) : 30; }
    public double getRuneKeeperCritMultiplier() { return config != null ? config.getDouble("runekeeper-settings.crit-multiplier", 1.5) : 1.5; }
    public int getRuneKeeperExpPerHit() { return config != null ? config.getInt("runekeeper-settings.exp-per-hit", 1) : 1; }
    public int getRuneKeeperExpPerDamage() { return config != null ? config.getInt("runekeeper-settings.exp-per-damage", 2) : 2; }
    public boolean isRuneKeeperNightVisionWithRunes() { return config != null ? config.getBoolean("runekeeper-settings.night-vision-with-runes", true) : true; }
    public boolean isRuneKeeperNoShields() { return config != null ? config.getBoolean("runekeeper-settings.no-shields", true) : true; }
    public boolean isRuneKeeperNoTotems() { return config != null ? config.getBoolean("runekeeper-settings.no-totems", true) : true; }
    public boolean isRuneKeeperShowBossbar() { return config != null ? config.getBoolean("runekeeper-settings.show-bossbar", true) : true; }

    public int getRuneKeeperStageExp(int stage) {
        if (config == null) return 5 * (stage + 1);
        return config.getInt("runekeeper-settings.stages." + stage + ".exp", 5 * (stage + 1));
    }

    public double getRuneKeeperStageHealth(int stage) {
        if (config == null) {
            double[] defaults = {7.0, 12.0, 17.0, 22.0, 27.0, 32.0};
            return defaults[Math.min(stage, 5)];
        }
        return config.getDouble("runekeeper-settings.stages." + stage + ".health", 7.0 + stage * 5);
    }

    public int getRuneKeeperStageSpeed(int stage) {
        if (config == null) {
            int[] defaults = {0, 0, 1, 1, 2, 2};
            return defaults[Math.min(stage, 5)];
        }
        return config.getInt("runekeeper-settings.stages." + stage + ".speed", stage / 2);
    }

    public double getRuneKeeperStageJump(int stage) {
        if (config == null) {
            double[] defaults = {0.75, 1.0, 1.25, 1.5, 1.75, 2.0};
            return defaults[Math.min(stage, 5)];
        }
        return config.getDouble("runekeeper-settings.stages." + stage + ".jump", 0.75 + stage * 0.25);
    }

    public double getRuneKeeperStageSize(int stage) {
        if (config == null) {
            double[] defaults = {1.25, 1.4, 1.55, 1.7, 1.85, 2.0};
            return defaults[Math.min(stage, 5)];
        }
        return config.getDouble("runekeeper-settings.stages." + stage + ".size", 1.25 + stage * 0.15);
    }

    // ==================== Respawn ====================
    public boolean hasRespawnLocation(String castName) {
        return config != null && config.contains("respawn." + castName);
    }

    public String getRespawnWorld(String castName) {
        return config != null ? config.getString("respawn." + castName + ".world", "world") : "world";
    }

    public double getRespawnX(String castName) {
        return config != null ? config.getDouble("respawn." + castName + ".x", 0.0) : 0.0;
    }

    public double getRespawnY(String castName) {
        return config != null ? config.getDouble("respawn." + castName + ".y", 64.0) : 64.0;
    }

    public double getRespawnZ(String castName) {
        return config != null ? config.getDouble("respawn." + castName + ".z", 0.0) : 0.0;
    }

    public double getRespawnRadius(String castName) {
        return config != null ? config.getDouble("respawn." + castName + ".radius", 0.0) : 0.0;
    }
    // ==================== Golem (Голем) - ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ====================
    public boolean isGolemPermanentSlowness() {
        return config != null ? config.getBoolean("golem-settings.permanent-slowness", true) : true;
    }
}
