package com.SheriffAnya.justRaces.events;

import com.SheriffAnya.justRaces.JustRaces;
import org.bukkit.event.Listener;

/**
 * Заглушка слушателя инвентаря.
 * Вся логика защиты особых предметов реализована в ArmorManager.
 */
public class InventoryListener implements Listener {

    private final JustRaces plugin;

    public InventoryListener(JustRaces plugin) {
        this.plugin = plugin;
    }
}
