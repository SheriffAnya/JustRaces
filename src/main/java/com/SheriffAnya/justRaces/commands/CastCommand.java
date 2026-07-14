package com.SheriffAnya.justRaces.commands;

import com.SheriffAnya.justRaces.JustRaces;
import com.SheriffAnya.justRaces.objects.Cast;
import com.SheriffAnya.justRaces.utils.MessageHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CastCommand implements CommandExecutor, TabCompleter {

    private final JustRaces plugin;

    public CastCommand(JustRaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleInfo(sender, args);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                return handleInfo(sender, args);
            case "list":
                return handleList(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                return handleInfo(sender, args);
        }
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length == 0 && sender instanceof Player) {
            Player player = (Player) sender;
            Cast cast = plugin.getCastManager().getPlayerCast(player.getName());

            if (cast == null) {
                sender.sendMessage(MessageHelper.error("У вас нет расы"));
                return true;
            }

            sendCastInfo(sender, cast);
            return true;
        } else if (args.length >= 1) {
            String arg = args[0];
            Cast cast = plugin.getCastManager().getCast(arg);

            if (cast != null) {
                sendCastInfo(sender, cast);
                return true;
            }

            Player target = Bukkit.getPlayer(arg);
            if (target != null) {
                cast = plugin.getCastManager().getPlayerCast(target.getName());
                if (cast != null) {
                    sender.sendMessage("§eРаса игрока " + target.getName() + ": §f" + cast.getName());
                    return true;
                } else {
                    sender.sendMessage(MessageHelper.error("У игрока нет расы"));
                    return true;
                }
            }

            // Проверяем оффлайн игрока по нику
            cast = plugin.getCastManager().getPlayerCast(arg);
            if (cast != null) {
                sender.sendMessage("§eРаса игрока " + arg + ": §f" + cast.getName());
                return true;
            }

            sender.sendMessage(MessageHelper.error("Раса или игрок не найдены"));
            return true;
        }

        return true;
    }

    private void sendCastInfo(CommandSender sender, Cast cast) {
        sender.sendMessage("§6=== Раса " + cast.getName() + " ===");
        sender.sendMessage("§eРазмер: §f" + cast.getCurrentSize() + "/" + cast.getMaxSize());
        sender.sendMessage("§eЗдоровье: §f" + cast.getHealth());
        sender.sendMessage("§eРазмер модели: §f" + cast.getSize());

        List<String> onlineMembers = new ArrayList<>();

        for (String memberName : cast.getMembersNames()) {
            Player member = Bukkit.getPlayer(memberName);
            if (member != null && member.isOnline()) {
                onlineMembers.add(memberName);
            }
        }

        if (!onlineMembers.isEmpty()) {
            sender.sendMessage("§eОнлайн: §f" + String.join(", ", onlineMembers));
        }
    }

    private boolean handleList(CommandSender sender) {
        Map<String, Cast> casts = plugin.getCastManager().getCasts();

        if (casts.isEmpty()) {
            sender.sendMessage("§cНет доступных рас");
            return true;
        }

        sender.sendMessage("§6=== Список рас ===");
        sender.sendMessage("§7Всего рас: §f" + casts.size());

        for (Cast cast : casts.values()) {
            int onlineCount = 0;
            for (String memberName : cast.getMembersNames()) {
                Player member = Bukkit.getPlayer(memberName);
                if (member != null && member.isOnline()) {
                    onlineCount++;
                }
            }

            String status = cast.isFull() ? "§c(Заполнена)" : "§a(Свободно)";
            String solo = cast.getMaxSize() == 1 ? "§d(Соло)" : "";
            sender.sendMessage("§7- §f" + cast.getName() + " §7[" + onlineCount + "/" + cast.getMaxSize() + "] " + status + " " + solo);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Команды рас ===");
        sender.sendMessage("§e/race §7- Ваша раса");
        sender.sendMessage("§e/race <раса> §7- Информация о расе");
        sender.sendMessage("§e/race <игрок> §7- Раса игрока");
        sender.sendMessage("§e/race list §7- Список рас");
        if (sender.hasPermission("race.admin")) {
            sender.sendMessage("§e/ra set <игрок> <раса> §7- Установить расу (админ)");
            sender.sendMessage("§e/ra add <игрок> <раса> §7- Добавить в расу (админ)");
            sender.sendMessage("§e/ra remove <игрок> §7- Удалить из расы (админ)");
            sender.sendMessage("§e/ra reload §7- Перезагрузить конфигурацию");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = Arrays.asList("info", "list", "help");

            // Все расы
            for (String castName : plugin.getCastManager().getCasts().keySet()) {
                if (castName.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(castName);
                }
            }

            // Игроки
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }

            // Команды
            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        }

        return completions;
    }
}
