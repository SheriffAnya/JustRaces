package com.SheriffAnya.justRaces.events;

import com.SheriffAnya.justRaces.JustRaces;
import com.SheriffAnya.justRaces.objects.Cast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class CastListener implements Listener {

    private final JustRaces plugin;
    private boolean debug;

    public CastListener(JustRaces plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (debug) plugin.getLogger().info("Игрок " + playerName + " зашел на сервер");

        if (!plugin.getCastManager().isInCast(playerName)) {
            boolean assigned = plugin.getCastManager().assignCast(player);

            if (assigned) {
                Cast cast = plugin.getCastManager().getPlayerCast(playerName);
                if (cast != null) {
                    player.sendActionBar("§aВы присоединены к расе: §e" + cast.getName());
                    if (debug) plugin.getLogger().info("Игроку " + playerName + " назначена раса " + cast.getName());
                }
            } else {
                if (debug) plugin.getLogger().info("Игроку " + playerName + " не удалось назначить расу");
            }
        } else {
            Cast cast = plugin.getCastManager().getPlayerCast(playerName);
            if (cast != null) {
                plugin.getCastManager().applyCastEffects(player, cast);
                plugin.getArmorManager().removeForbiddenItems(player);
                if (debug) plugin.getLogger().info("Игроку " + playerName + " применены эффекты расы " + cast.getName());
            }
        }
    }
}
