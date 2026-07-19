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

    private static final Map<String, String> RACE_DESCRIPTIONS = new HashMap<>();

    static {
        RACE_DESCRIPTIONS.put("Русал", "Может дышать под водой без ограничений (а также под дождём или рядом с активным морским источником), но вне воды кислород истощается быстрее и удушье наносит урон и замедляет. В воде получает эффект дельфиней грации и ускорение добычи. Владеет уникальным незаменяемым трезубцем (Верность, Пронзание, Прикованность). Сырая рыба хорошо насыщает, обычная еда вызывает голод.");
        RACE_DESCRIPTIONS.put("Элей", "Маленькая и хрупкая раса, постоянно летающая с помощью волшебного пера: до 40 секунд полёта с перезарядкой, не получает урон от падения. Раз в 20 секунд получает заряд ветра. При столкновении в PvP на 15 секунд включается штрафной режим — расход времени полёта удваивается, а скорость падает вдвое.");
        RACE_DESCRIPTIONS.put("Фортуна", "Обладает постоянной удачей и наносит больше урона, чем сильнее ранена сама. При смертельном ударе с шансом 30% срабатывает спасительный «тотем» — исцеление, регенерация, огнестойкость и временная слепота.");
        RACE_DESCRIPTIONS.put("Нежить", "Получает урон и поджигается от солнечного света, отлично видит в темноте, а на свету слепнет. Обычная еда вредит, а гнилая плоть и паучьи глаза восстанавливают сытость без штрафов; лечащие зелья наносят двойной урон. Владеет уникальным незаменяемым луком (Бесконечность, Пламя, Сила). Нежить-мобы её не атакуют.");
        RACE_DESCRIPTIONS.put("Голем", "Крупная и медленная, носит несъёмную неломающуюся железную броню, имеет постоянное сопротивление урону. Не получает отбрасывания от ударов, а при ударе отбрасывает атакующего.");
        RACE_DESCRIPTIONS.put("Варден", "Самая живучая раса — 50 здоровья, крупный размер, постоянное Сопротивление. Получает бонус урона и силы рядом со скалком, а удары могут оглушать противника. Раз в 30 секунд получает \"Осколок эха\" для подсветки существ поблизости. Не может носить броню.");
        RACE_DESCRIPTIONS.put("Эндермен", "Крупная раса с увеличенной дальностью атаки. Раз в 20 секунд получает эндер-жемчуг — телепортация даёт кратковременную неуязвимость и шанс призвать эндермита. Уязвима к воде и дождю, но игнорирует обычное утопление. Плоды хоруса хорошо насыщают, обычная еда — плохо. Эндермены её не трогают.");
        RACE_DESCRIPTIONS.put("Гигант", "Самая крупная из обычных рас. Постоянно ускоренно копает, удар голым кулаком наносит фиксированный урон с усиленным отбрасыванием. Не может спать в одиночной кровати — нужна кровать 2×2.");
        RACE_DESCRIPTIONS.put("Тень", "Раз в 2.5 минуты получает уникальное зелье невидимости на 2 минуты — пока невидима, получает силу и не горит, но становится слабой, если видима. Скрытая атака наносит полный урон и накладывает Иссушение, обычная — лишь четверть урона. Голод падает быстрее обычного.");
        RACE_DESCRIPTIONS.put("Друид", "В лесных и таёжных биомах получает силу и ускорение, на траве и мхе — регенерацию. В Аду постоянно замедлена, а огонь наносит ей повышенный урон. Раз в 2 минуты может призвать двух волков-компаньонов на помощь.");
        RACE_DESCRIPTIONS.put("Эфрит", "Иммунитет к лаве и огню, но получает урон от воды и дождя. В Аду получает ночное зрение, ускорение, силу и регенерацию у огня, а адские мобы её не атакуют. Владеет уникальным огненным шаром с перезарядкой.");
        RACE_DESCRIPTIONS.put("ХранительРун", "Растёт вместе с уровнем игрока — от 7 до 32 здоровья и от маленького до крупного размера по шести стадиям. Копит до 3 рун (одна восстанавливается раз в 30 секунд), каждая руна полностью поглощает один удар; без рун урон убавляет опыт. На максимуме рун атаки усилены и даётся постоянное ночное зрение. Соло-раса, не может носить броню, щиты и тотемы бессмертия.");
    }

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
        sender.sendMessage("§eУчастники: §f" + cast.getCurrentSize() + "/" + cast.getMaxSize());
        sender.sendMessage("§eЗдоровье: §f" + cast.getHealth());
        sender.sendMessage("§eРазмер модели: §f" + cast.getSize());

        String description = RACE_DESCRIPTIONS.get(cast.getName());
        if (description != null) {
            sender.sendMessage("§eСпособности:");
            for (String line : wrapText(description, 60)) {
                sender.sendMessage("§7" + line);
            }
        }

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

    private List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split(" ")) {
            if (currentLine.length() + word.length() + 1 > maxLineLength) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
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
