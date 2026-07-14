package com.SheriffAnya.justRaces.events;

import com.SheriffAnya.justRaces.JustRaces;
import com.SheriffAnya.justRaces.objects.Cast;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final JustRaces plugin;

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("§[0-9a-fklmnor]");
    private static final Pattern AMPERSAND_COLOR_PATTERN = Pattern.compile("&[0-9a-fklmnor]");

    public ChatListener(JustRaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // 👇 ПРОВЕРКА - ЕСЛИ ЧАТ ОТКЛЮЧЕН В КОНФИГЕ, НЕ ОБРАБАТЫВАЕМ
        if (!plugin.isChatEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Cast cast = plugin.getCastManager().getPlayerCast(player.getName());

        String playerName = getPlayerDisplayName(player);

        String rawMessage = event.getMessage();
        String message = rawMessage;

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        if (cast != null) {
            boolean canUseColors = player.hasPermission("race.chat.color");

            if (!canUseColors) {
                message = stripDirectColorCodes(rawMessage);
            }

            String format = plugin.getConfig().getString("chat.global_format", "{cast_prefix} {player}: {message}");
            String formattedMessage = format
                    .replace("{cast_prefix}", cast.getColor() + "[" + cast.getName() + "]")
                    .replace("{player}", playerName)
                    .replace("{message}", message);

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                formattedMessage = PlaceholderAPI.setPlaceholders(player, formattedMessage);
            }

            event.setFormat(ChatColor.translateAlternateColorCodes('&', formattedMessage));
        } else {
            String format = playerName + ": " + message;
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                format = PlaceholderAPI.setPlaceholders(player, format);
            }
            event.setFormat(format);
        }
    }

    private String stripDirectColorCodes(String input) {
        if (input == null || input.isEmpty()) return input;

        String result = input;
        result = AMPERSAND_COLOR_PATTERN.matcher(result).replaceAll("");
        result = LEGACY_COLOR_PATTERN.matcher(result).replaceAll("");

        return result;
    }

    private String getPlayerDisplayName(Player player) {
        String displayName = player.getName();

        try {
            String playerDisplayName = player.getDisplayName();
            if (!playerDisplayName.equals(player.getName())) {
                String processedName = playerDisplayName;
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    processedName = PlaceholderAPI.setPlaceholders(player, playerDisplayName);
                }
                return ChatColor.translateAlternateColorCodes('&', processedName);
            }
        } catch (Exception e) {
        }

        if (player.isOp()) {
            return "§c" + player.getName() + "§f";
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            displayName = PlaceholderAPI.setPlaceholders(player, displayName);
        }

        return displayName;
    }
}
