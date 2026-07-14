package com.SheriffAnya.justRaces.placeholders;

import com.SheriffAnya.justRaces.JustRaces;
import com.SheriffAnya.justRaces.objects.Cast;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CastPlaceholder extends PlaceholderExpansion {

    private final JustRaces plugin;

    private static final java.util.Map<String, String> legacyToMiniMessage = new java.util.HashMap<>();

    static {
        legacyToMiniMessage.put("&0", "<black>");
        legacyToMiniMessage.put("&1", "<dark_blue>");
        legacyToMiniMessage.put("&2", "<dark_green>");
        legacyToMiniMessage.put("&3", "<dark_aqua>");
        legacyToMiniMessage.put("&4", "<dark_red>");
        legacyToMiniMessage.put("&5", "<dark_purple>");
        legacyToMiniMessage.put("&6", "<gold>");
        legacyToMiniMessage.put("&7", "<gray>");
        legacyToMiniMessage.put("&8", "<dark_gray>");
        legacyToMiniMessage.put("&9", "<blue>");
        legacyToMiniMessage.put("&a", "<green>");
        legacyToMiniMessage.put("&b", "<aqua>");
        legacyToMiniMessage.put("&c", "<red>");
        legacyToMiniMessage.put("&d", "<light_purple>");
        legacyToMiniMessage.put("&e", "<yellow>");
        legacyToMiniMessage.put("&f", "<white>");

        legacyToMiniMessage.put("&k", "<obfuscated>");
        legacyToMiniMessage.put("&l", "<bold>");
        legacyToMiniMessage.put("&m", "<strikethrough>");
        legacyToMiniMessage.put("&n", "<underlined>");
        legacyToMiniMessage.put("&o", "<italic>");
        legacyToMiniMessage.put("&r", "<reset>");
    }

    public CastPlaceholder(JustRaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cast";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SheriffAnya";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.18";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        if (player.getName() == null) return "";

        Cast cast = plugin.getCastManager().getPlayerCast(player.getName());

        switch (params.toLowerCase()) {
            case "name":
                return cast != null ? cast.getName() : "";

            case "color":
                if (cast == null) return "";
                String legacyColor = cast.getColor();
                return convertLegacyToMiniMessage(legacyColor);

            case "formatted_name":
                if (cast == null) return "";
                String legacyFormatted = cast.getColor().replace('&', '§') + cast.getName();
                return convertLegacyToMiniMessage(legacyFormatted);

            case "prefix":
                if (cast == null) return "";
                String legacyPrefix = cast.getColor().replace('&', '§') + "[" + cast.getName() + "]";
                return convertLegacyToMiniMessage(legacyPrefix);

            case "members":
                return cast != null ? String.valueOf(cast.getCurrentSize()) : "0";

            case "max_members":
                return cast != null ? String.valueOf(cast.getMaxSize()) : "0";

            case "health":
                return cast != null ? String.valueOf(cast.getHealth()) : "20";

            case "size":
                return cast != null ? String.valueOf(cast.getSize()) : "1.0";

            case "has_cast":
                return cast != null ? "да" : "нет";

            case "is_full":
                return cast != null ? (cast.isFull() ? "да" : "нет") : "нет";

            case "online_count":
                if (cast == null) return "0";
                int online = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (cast.hasMember(p.getName())) online++;
                }
                return String.valueOf(online);

            case "status":
                if (cast == null) return "без расы";
                String legacyStatus = cast.getColor().replace('&', '§') + cast.getName();
                return convertLegacyToMiniMessage(legacyStatus);

            case "color_code":
                if (cast == null) return "";
                return cast.getColor().replace("&", "");

            case "chat_prefix":
                if (cast == null) return "";
                String format = plugin.getConfig().getString("chat.format", "{cast_prefix} {player}: {message}");
                String legacyChatPrefix = format.split("\\{player\\}")[0].replace("{cast_prefix}", cast.getColor() + "[" + cast.getName() + "]");
                return convertLegacyToMiniMessage(legacyChatPrefix);
        }

        if (params.toLowerCase().startsWith("has_member_")) {
            String targetName = params.substring("has_member_".length());
            if (cast != null) {
                return cast.hasMember(targetName) ? "да" : "нет";
            }
            return "нет";
        }

        return null;
    }

    private String convertLegacyToMiniMessage(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) return "";

        String text = legacyText.replace('§', '&');

        for (java.util.Map.Entry<String, String> entry : legacyToMiniMessage.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        text = text.replace("&", "");

        return text;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        List<String> placeholders = new ArrayList<>();
        placeholders.add("%cast_name%");
        placeholders.add("%cast_color%");
        placeholders.add("%cast_formatted_name%");
        placeholders.add("%cast_prefix%");
        placeholders.add("%cast_members%");
        placeholders.add("%cast_max_members%");
        placeholders.add("%cast_health%");
        placeholders.add("%cast_size%");
        placeholders.add("%cast_has_cast%");
        placeholders.add("%cast_is_full%");
        placeholders.add("%cast_online_count%");
        placeholders.add("%cast_status%");
        placeholders.add("%cast_color_code%");
        placeholders.add("%cast_chat_prefix%");
        placeholders.add("%cast_has_member_<имя>%");
        return placeholders;
    }
}
