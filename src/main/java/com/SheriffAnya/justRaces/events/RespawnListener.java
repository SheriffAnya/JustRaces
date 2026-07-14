package com.SheriffAnya.justRaces.events;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RespawnListener implements Listener {

    private final JustRaces plugin;
    private final Random random = new Random();

    public RespawnListener(JustRaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (player.getRespawnLocation() != null && event.isBedSpawn()) {
            applySlowFalling(player);
            return;
        }

        String castName = plugin.getCastManager().getPlayerCastName(player.getName());
        if (castName == null) {
            applySlowFalling(player);
            return;
        }

        String path = "respawn." + castName;
        if (!plugin.getConfig().contains(path)) {
            applySlowFalling(player);
            return;
        }

        try {
            double x = plugin.getConfig().getDouble(path + ".x");
            double y = plugin.getConfig().getDouble(path + ".y");
            double z = plugin.getConfig().getDouble(path + ".z");
            String worldName = plugin.getConfig().getString(path + ".world");
            double radius = plugin.getConfig().getDouble(path + ".radius", 0);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                applySlowFalling(player);
                return;
            }

            double finalX = x;
            double finalZ = z;

            if (radius > 0) {
                double offsetX = (random.nextDouble() - 0.5) * 2 * radius;
                double offsetZ = (random.nextDouble() - 0.5) * 2 * radius;
                finalX = x + offsetX;
                finalZ = z + offsetZ;
            }

            Location respawnLoc = new Location(world, finalX, y, finalZ);
            respawnLoc = findSafeLocation(respawnLoc);
            event.setRespawnLocation(respawnLoc);

        } catch (Exception ignored) {}

        applySlowFalling(player);
    }

    private Location findSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return location;

        Location safeLoc = location.clone();
        double originalY = location.getY();

        for (int y = 0; y < 5; y++) {
            safeLoc.setY(originalY + y);

            if (!safeLoc.getBlock().isPassable() &&
                    safeLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
                return safeLoc.clone().add(0.5, 1, 0.5);
            }
        }

        for (int y = 0; y < 10; y++) {
            safeLoc.setY(originalY + y);
            if (safeLoc.getBlock().isPassable() &&
                    safeLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
                return safeLoc.clone().add(0.5, 0, 0.5);
            }
        }

        return location.add(0.5, 1, 0.5);
    }

    private void applySlowFalling(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player != null && player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOW_FALLING,
                            5 * 20,
                            0,
                            false,
                            false
                    ));
                }
            }
        }.runTaskLater(plugin, 2L);
    }
}
