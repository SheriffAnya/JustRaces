package com.SheriffAnya.justRaces.commands;

import com.SheriffAnya.justRaces.JustRaces;
import com.SheriffAnya.justRaces.objects.Cast;
import com.SheriffAnya.justRaces.utils.MessageHelper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CastAdminCommand implements CommandExecutor, TabCompleter {

    private final JustRaces plugin;
    private boolean debug;

    public CastAdminCommand(JustRaces plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("race.admin")) {
            sender.sendMessage(MessageHelper.error("У вас нет прав на использование этой команды"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                return handleSet(sender, args);
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "reload":
                plugin.reloadConfig();
                plugin.getConfigManager().loadConfig();
                plugin.getCastManager().loadConfig();
                plugin.getDataManager().loadCasts();
                sender.sendMessage("§aКонфигурация перезагружена");
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageHelper.error("Использование: /ra set <игрок> <раса>"));
            return true;
        }

        String targetName = args[1];
        String castName = args[2];

        Cast cast = plugin.getCastManager().getCast(castName);
        if (cast == null) {
            sender.sendMessage(MessageHelper.error("Раса не найдена"));
            return true;
        }

        // Проверяем соло-расу
        if (cast.getMaxSize() == 1 && !cast.isEmpty() && !cast.hasMember(targetName)) {
            sender.sendMessage(MessageHelper.error("Соло-раса '" + castName + "' уже занята"));
            return true;
        }

        plugin.getCastManager().setPlayerCast(targetName, castName);
        sender.sendMessage("§aИгрок " + targetName + " установлен в расу " + castName);
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageHelper.error("Использование: /ra add <игрок> <раса>"));
            return true;
        }

        String targetName = args[1];
        String castName = args[2];

        Cast cast = plugin.getCastManager().getCast(castName);
        if (cast == null) {
            sender.sendMessage(MessageHelper.error("Раса не найдена"));
            return true;
        }

        // Проверяем соло-расу
        if (cast.getMaxSize() == 1 && !cast.isEmpty()) {
            sender.sendMessage(MessageHelper.error("Соло-раса '" + castName + "' уже занята"));
            return true;
        }

        // Проверяем, не полна ли раса
        if (cast.isFull() && cast.getMaxSize() != 1) {
            sender.sendMessage(MessageHelper.error("Раса уже заполнена"));
            return true;
        }

        // Проверяем, не состоит ли игрок уже в этой расе
        if (cast.hasMember(targetName)) {
            sender.sendMessage(MessageHelper.error("Игрок уже состоит в этой расе"));
            return true;
        }

        // Если игрок онлайн - применяем эффекты сразу
        Player onlinePlayer = Bukkit.getPlayer(targetName);

        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            plugin.getCastManager().setPlayerCast(targetName, castName);
            sender.sendMessage("§aИгрок " + targetName + " добавлен в расу " + castName);
        } else {
            // Если игрок оффлайн - просто сохраняем в данные
            Cast oldCast = plugin.getCastManager().getPlayerCast(targetName);
            if (oldCast != null) {
                oldCast.removeMember(targetName);
            }

            if (cast.addMember(targetName)) {
                plugin.getCastManager().getPlayerCastsMap().put(targetName, castName);
                plugin.getDataManager().saveCastsAsync();
                sender.sendMessage("§aИгрок " + targetName + " добавлен в расу " + castName + " (будет применено при входе)");
                if (debug) plugin.getLogger().info("Добавлен оффлайн игрок " + targetName + " в расу " + castName);
            } else {
                sender.sendMessage(MessageHelper.error("Не удалось добавить игрока в расу"));
            }
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageHelper.error("Использование: /ra remove <игрок>"));
            return true;
        }

        String targetName = args[1];
        Cast cast = plugin.getCastManager().getPlayerCast(targetName);

        if (cast == null) {
            sender.sendMessage(MessageHelper.error("Игрок не состоит в расе"));
            return true;
        }

        // Сначала удаляем эффекты, пока запись в playerCasts ещё есть (иначе removeCastEffects не найдёт расу)
        Player onlinePlayer = Bukkit.getPlayer(targetName);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            plugin.getCastManager().removeCastEffects(onlinePlayer);
        }

        cast.removeMember(targetName);
        plugin.getCastManager().getPlayerCastsMap().remove(targetName);

        plugin.getDataManager().saveCastsAsync();

        sender.sendMessage("§aИгрок " + targetName + " удален из расы " + cast.getName());
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Админ команды рас ===");
        sender.sendMessage("§e/ra set <игрок> <раса> §7- Установить расу игроку");
        sender.sendMessage("§e/ra add <игрок> <раса> §7- Добавить игрока в расу");
        sender.sendMessage("§e/ra remove <игрок> §7- Удалить игрока из расы");
        sender.sendMessage("§e/ra reload §7- Перезагрузить конфигурацию");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("race.admin")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> commands = Arrays.asList("set", "add", "remove", "reload");
            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            // Добавляем оффлайн игроков в таб-комплит
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            for (String castName : plugin.getCastManager().getCasts().keySet()) {
                if (castName.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(castName);
                }
            }
        }

        return completions;
    }
}
