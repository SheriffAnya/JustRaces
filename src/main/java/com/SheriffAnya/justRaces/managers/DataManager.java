package com.SheriffAnya.justRaces.managers;

import com.SheriffAnya.justRaces.JustRaces;
import com.SheriffAnya.justRaces.objects.Cast;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final JustRaces plugin;
    private File castsFile;
    private FileConfiguration castsConfig;
    private boolean debug;

    public DataManager(JustRaces plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    public void load() {
        setupFiles();
        loadCasts();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        castsFile = new File(plugin.getDataFolder(), "casts.yml");

        if (!castsFile.exists()) {
            createDefaultFile();
        }

        castsConfig = YamlConfiguration.loadConfiguration(castsFile);
    }

    private void createDefaultFile() {
        try {
            castsFile.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(castsFile);

            String[] races = {"Русал", "Элей", "Фортуна", "Нежить", "Голем", "Варден",
                    "Эндермен", "Гигант", "Тень", "Друид", "Эфрит", "ХранительРун"};

            for (String race : races) {
                config.set("casts." + race + ".members", new ArrayList<String>());
            }

            config.save(castsFile);
            if (debug) plugin.getLogger().info("Создан файл casts.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось создать файл casts.yml: " + e.getMessage());
        }
    }

    public void loadCasts() {
        if (castsConfig == null) {
            castsConfig = YamlConfiguration.loadConfiguration(castsFile);
        }

        // Очищаем все расы
        for (Cast cast : plugin.getCastManager().getCasts().values()) {
            cast.clearMembers();
        }
        plugin.getCastManager().getPlayerCastsMap().clear();

        if (!castsConfig.contains("casts")) {
            if (debug) plugin.getLogger().info("Нет секции casts в файле, создаю новую");
            saveCasts();
            return;
        }

        org.bukkit.configuration.ConfigurationSection castsSection = castsConfig.getConfigurationSection("casts");
        if (castsSection == null) {
            if (debug) plugin.getLogger().warning("Секция casts повреждена или имеет неверный формат, создаю новую");
            saveCasts();
            return;
        }

        for (String castName : castsSection.getKeys(false)) {
            Cast cast = plugin.getCastManager().getCast(castName);
            if (cast == null) {
                if (debug) plugin.getLogger().warning("Раса '" + castName + "' не найдена в конфиге");
                continue;
            }

            String path = "casts." + castName + ".members";

            if (castsConfig.contains(path) && castsConfig.isList(path)) {
                List<String> memberNames = castsConfig.getStringList(path);
                for (String playerName : memberNames) {
                    if (playerName == null || playerName.trim().isEmpty()) continue;

                    playerName = playerName.trim();

                    // Сохраняем по НИКУ, не конвертируем в UUID!
                    cast.addMember(playerName);
                    plugin.getCastManager().getPlayerCastsMap().put(playerName, castName);

                    if (debug) plugin.getLogger().info("Загружен игрок " + playerName + " в расу " + castName);
                }
            }
        }

        if (debug) {
            plugin.getLogger().info("Загрузка завершена. Всего игроков в расах: " +
                    plugin.getCastManager().getPlayerCastsMap().size());
        }
    }

    /**
     * Синхронное сохранение (блокирует главный поток дисковым I/O).
     * Использовать только там, где нужна гарантия записи до продолжения
     * выполнения — например, в onDisable() при выключении сервера.
     */
    public void saveCasts() {
        if (castsConfig == null || castsFile == null) return;

        fillConfigFromCasts();

        try {
            castsConfig.save(castsFile);
            if (debug) plugin.getLogger().info("Сохранены данные рас в файл");
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка при сохранении casts.yml: " + e.getMessage());
        }
    }

    /**
     * Асинхронное сохранение: снимок данных рас снимается на главном потоке
     * (это дёшево — просто копирование списков ников), а сама запись на
     * диск уходит в отдельный поток, чтобы не блокировать тик сервера.
     * Используется для всех сохранений во время работы сервера (после
     * /race set, /raceadmin и т.п.), кроме выключения сервера.
     */
    public void saveCastsAsync() {
        if (castsConfig == null || castsFile == null) return;

        fillConfigFromCasts();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                castsConfig.save(castsFile);
                if (debug) plugin.getLogger().info("Сохранены данные рас в файл (асинхронно)");
            } catch (IOException e) {
                plugin.getLogger().severe("Ошибка при сохранении casts.yml: " + e.getMessage());
            }
        });
    }

    private void fillConfigFromCasts() {
        for (Cast cast : plugin.getCastManager().getCasts().values()) {
            String path = "casts." + cast.getName();

            // Сохраняем по НИКАМ
            List<String> memberNames = new ArrayList<>(cast.getMembersNames());
            castsConfig.set(path + ".members", memberNames);
        }
    }

    public void saveCastsSync() {
        saveCasts();
    }

    public void updatePlayerData(String playerName) {
        saveCastsAsync();
    }
}
