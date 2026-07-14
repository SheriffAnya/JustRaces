package com.SheriffAnya.justRaces;

import com.SheriffAnya.justRaces.commands.*;
import com.SheriffAnya.justRaces.casts.*;
import com.SheriffAnya.justRaces.events.CastListener;
import com.SheriffAnya.justRaces.events.ChatListener;
import com.SheriffAnya.justRaces.events.InventoryListener;
import com.SheriffAnya.justRaces.events.RespawnListener;
import com.SheriffAnya.justRaces.managers.ArmorManager;
import com.SheriffAnya.justRaces.managers.CastManager;
import com.SheriffAnya.justRaces.managers.ConfigManager;
import com.SheriffAnya.justRaces.managers.DataManager;
import com.SheriffAnya.justRaces.placeholders.CastPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class JustRaces extends JavaPlugin {

    private static JustRaces instance;
    private ConfigManager configManager;
    private CastManager castManager;
    private DataManager dataManager;
    private ArmorManager armorManager;
    private CastPlaceholder placeholder;
    private boolean debug;
    private boolean chatEnabled;  // 👈 НОВОЕ ПОЛЕ

    private RusalCast rusalCast;
    private EnderCast enderCast;
    private UndeadCast undeadCast;
    private EleiCast eleiCast;
    private ShadowCast shadowCast;
    private EffritCast effritCast;
    private GiantCast giantCast;
    private FortuneCast fortuneCast;
    private GolemCast golemCast;
    private WardenCast wardenCast;
    private DruidCast druidCast;
    private RuneKeeperCast runeKeeperCast;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Сначала загружаем конфиг, чтобы получить debug и chatEnabled
            saveDefaultConfig();
            reloadConfig();
            debug = getConfig().getBoolean("debug", false);
            chatEnabled = getConfig().getBoolean("chat.enabled", true);  // 👈 ЗАГРУЖАЕМ НАСТРОЙКУ

            this.configManager = new ConfigManager(this);
            this.castManager = new CastManager(this);
            this.dataManager = new DataManager(this);
            this.armorManager = new ArmorManager(this);

            configManager.loadConfig();
            castManager.initialize();
            dataManager.load();
            castManager.loadPlayerCasts();

            if (debug) getLogger().info("Загружены данные игроков: " + castManager.getPlayerCastsMap().size() + " записей");
            if (debug) getLogger().info("Чат плагина: " + (chatEnabled ? "ВКЛЮЧЕН" : "ВЫКЛЮЧЕН"));

            // Инициализация рас
            this.rusalCast = new RusalCast(this);
            this.enderCast = new EnderCast(this);
            this.undeadCast = new UndeadCast(this);
            this.eleiCast = new EleiCast(this);
            this.shadowCast = new ShadowCast(this);
            this.effritCast = new EffritCast(this);
            this.giantCast = new GiantCast(this);
            this.fortuneCast = new FortuneCast(this);
            this.golemCast = new GolemCast(this);
            this.wardenCast = new WardenCast(this);
            this.druidCast = new DruidCast(this);
            this.runeKeeperCast = new RuneKeeperCast(this);

            // Регистрация команд
            registerCommand("race", new CastCommand(this));
            registerCommand("raceadmin", new CastAdminCommand(this));

            // Регистрация событий
            Bukkit.getPluginManager().registerEvents(new CastListener(this), this);
            Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);

            // 👇 ТОЛЬКО ЕСЛИ ЧАТ ВКЛЮЧЕН
            if (chatEnabled) {
                Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
                if (debug) getLogger().info("ChatListener зарегистрирован");
            } else {
                if (debug) getLogger().info("ChatListener НЕ зарегистрирован (чат отключен в конфиге)");
            }

            Bukkit.getPluginManager().registerEvents(new RespawnListener(this), this);
            Bukkit.getPluginManager().registerEvents(armorManager, this);

            Bukkit.getPluginManager().registerEvents(rusalCast, this);
            Bukkit.getPluginManager().registerEvents(enderCast, this);
            Bukkit.getPluginManager().registerEvents(undeadCast, this);
            Bukkit.getPluginManager().registerEvents(eleiCast, this);
            Bukkit.getPluginManager().registerEvents(shadowCast, this);
            Bukkit.getPluginManager().registerEvents(effritCast, this);
            Bukkit.getPluginManager().registerEvents(giantCast, this);
            Bukkit.getPluginManager().registerEvents(fortuneCast, this);
            Bukkit.getPluginManager().registerEvents(golemCast, this);
            Bukkit.getPluginManager().registerEvents(wardenCast, this);
            Bukkit.getPluginManager().registerEvents(druidCast, this);
            Bukkit.getPluginManager().registerEvents(runeKeeperCast, this);

            // Применение эффектов для онлайн игроков
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String castName = castManager.getPlayerCastName(player.getName());
                    if (castName != null) {
                        castManager.applyCastEffects(player, castManager.getCast(castName));
                        armorManager.removeForbiddenItems(player);
                        if (debug) getLogger().info("Применены эффекты для игрока " + player.getName() + " (" + castName + ")");
                    }
                }
            }, 20L);

            // PlaceholderAPI
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholder = new CastPlaceholder(this);
                placeholder.register();
                if (debug) getLogger().info("PlaceholderAPI зарегистрирован");
            }

            getLogger().info("JustRaces включен! Загружено рас: " + castManager.getCasts().size());
            getLogger().info("Чат плагина: " + (chatEnabled ? "ВКЛЮЧЕН" : "ВЫКЛЮЧЕН"));

        } catch (Exception e) {
            getLogger().severe("Ошибка: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Безопасная регистрация команды с защитой от NPE.
     * getCommand() может вернуть null, если команда не объявлена в plugin.yml
     * или PluginCommand не был создан по какой-то причине.
     */
    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("Не удалось зарегистрировать команду /" + name + " — она не найдена в plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveCasts();
            if (debug) getLogger().info("Данные рас сохранены");
        }

        if (castManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String castName = castManager.getPlayerCastName(player.getName());
                if (castName != null) {
                    castManager.removeCastEffects(player);
                }
            }
        }

        if (placeholder != null) {
            placeholder.unregister();
        }

        getLogger().info("JustRaces выключен!");
    }

    // Getters
    public static JustRaces getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public CastManager getCastManager() { return castManager; }
    public DataManager getDataManager() { return dataManager; }
    public ArmorManager getArmorManager() { return armorManager; }
    public boolean isChatEnabled() { return chatEnabled; }  // 👈 НОВЫЙ ГЕТТЕР

    public RusalCast getRusalCast() { return rusalCast; }
    public EnderCast getEnderCast() { return enderCast; }
    public UndeadCast getUndeadCast() { return undeadCast; }
    public EleiCast getEleiCast() { return eleiCast; }
    public ShadowCast getShadowCast() { return shadowCast; }
    public EffritCast getEffritCast() { return effritCast; }
    public GiantCast getGiantCast() { return giantCast; }
    public FortuneCast getFortuneCast() { return fortuneCast; }
    public GolemCast getGolemCast() { return golemCast; }
    public WardenCast getWardenCast() { return wardenCast; }
    public DruidCast getDruidCast() { return druidCast; }
    public RuneKeeperCast getRuneKeeperCast() { return runeKeeperCast; }
}
