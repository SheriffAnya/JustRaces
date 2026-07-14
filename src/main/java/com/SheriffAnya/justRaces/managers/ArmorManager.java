package com.SheriffAnya.justRaces.managers;

import com.SheriffAnya.justRaces.JustRaces;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Predicate;

public class ArmorManager implements Listener {

    private final JustRaces plugin;

    // Множества рас, для которых запрещены определённые типы предметов
    private final Set<String> noArmorRaces = new HashSet<>(Arrays.asList(
            "Варден"
    ));
    private final Set<String> noTotemRaces = new HashSet<>(Arrays.asList(
            "ХранительРун"
    ));
    private final Set<String> noShieldRaces = new HashSet<>(Arrays.asList(
            "ХранительРун"
    ));

    // Карта для задач проверки мешков (Bundle)
    private final Map<UUID, Integer> bundleCheckTasks = new HashMap<>();

    public ArmorManager(JustRaces plugin) {
        this.plugin = plugin;
    }

    // ===================== БАЗОВЫЕ ПРОВЕРКИ =====================

    private boolean canWearArmor(Player player) {
        String castName = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName == null || !noArmorRaces.contains(castName);
    }

    private boolean canUseTotem(Player player) {
        String castName = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName == null || !noTotemRaces.contains(castName);
    }

    private boolean canUseShield(Player player) {
        String castName = plugin.getCastManager().getPlayerCastName(player.getName());
        return castName == null || !noShieldRaces.contains(castName);
    }

    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type.name().endsWith("_HELMET") ||
                type.name().endsWith("_CHESTPLATE") ||
                type.name().endsWith("_LEGGINGS") ||
                type.name().endsWith("_BOOTS");
    }

    private boolean isTotem(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING;
    }

    private boolean isShield(ItemStack item) {
        return item != null && item.getType() == Material.SHIELD;
    }

    private boolean isBundle(ItemStack item) {
        return item != null && item.getType() == Material.BUNDLE;
    }

    /**
     * Проверка, является ли предмет особым предметом расы.
     * Используется для запрета перемещения/выброса/передачи.
     */
    private boolean isCustomRaceItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String displayName = item.getItemMeta().getDisplayName();

        return displayName.equals("§bВолшебный трезубец Русала") ||
                displayName.equals("§eВолшебное перо Элея") ||
                displayName.equals("§4Лук Нежити") ||
                displayName.equals("§6Огненный шар Эфрита") ||
                displayName.equals("§3Осколок эха Вардена") ||
                displayName.equals("§bЗаряд ветра") ||
                displayName.equals("§8Зелье невидимости Тени") ||
                displayName.equals("§5Жемчуг Эндермена") ||
                displayName.equals("§aКость призыва Друида") ||
                (item.getType() == Material.IRON_HELMET && displayName.equals("§7Шлем Голема")) ||
                (item.getType() == Material.IRON_CHESTPLATE && displayName.equals("§7Нагрудник Голема")) ||
                (item.getType() == Material.IRON_LEGGINGS && displayName.equals("§7Поножи Голема")) ||
                (item.getType() == Material.IRON_BOOTS && displayName.equals("§7Ботинки Голема"));
    }

    private boolean isForbiddenForPlayer(Player player, ItemStack item) {
        if (item == null) return false;
        return (isArmor(item) && !canWearArmor(player)) ||
                (isTotem(item) && !canUseTotem(player)) ||
                (isShield(item) && !canUseShield(player));
    }

    // ===================== МЕТОДЫ УДАЛЕНИЯ ЗАПРЕЩЁННЫХ ПРЕДМЕТОВ =====================

    public void removeForbiddenItems(Player player) {
        String castName = plugin.getCastManager().getPlayerCastName(player.getName());
        if (castName == null) return;

        PlayerInventory inv = player.getInventory();

        if (!canWearArmor(player)) {
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null && isArmor(armor[i])) {
                    player.getWorld().dropItem(player.getLocation(), armor[i].clone());
                    armor[i] = null;
                }
            }
            inv.setArmorContents(armor);
        }

        if (!canUseTotem(player)) {
            removeMatchingItemsFromInventory(player, this::isTotem);
        }

        if (!canUseShield(player)) {
            removeMatchingItemsFromInventory(player, this::isShield);
        }

        // Особые предметы рас не удаляем автоматически (они должны оставаться у игрока)
        player.updateInventory();
    }

    private void removeMatchingItemsFromInventory(Player player, Predicate<ItemStack> matcher) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && matcher.test(item)) {
                player.getWorld().dropItem(player.getLocation(), item.clone());
                inv.setItem(i, null);
            }
        }
        ItemStack offHand = inv.getItemInOffHand();
        if (matcher.test(offHand)) {
            player.getWorld().dropItem(player.getLocation(), offHand.clone());
            inv.setItemInOffHand(null);
        }
    }

    // ===================== ЗАЩИТА МЕШКА (BUNDLE) =====================

    /**
     * Запускает периодическую проверку мешков в инвентаре игрока.
     * Вызывается при применении эффектов расы.
     */
    public void startBundleCheck(Player player) {
        UUID playerId = player.getUniqueId();
        stopBundleCheck(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline() || plugin.getCastManager().getPlayerCast(player.getName()) == null) {
                stopBundleCheck(player);
                return;
            }
            // Проверяем все слоты инвентаря на наличие мешков
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && isBundle(item)) {
                    ItemStack cleaned = cleanBundle(item, player);
                    if (cleaned != item) {
                        player.getInventory().setItem(i, cleaned);
                    }
                }
            }
        }, 0L, 40L); // проверка каждые 2 секунды

        bundleCheckTasks.put(playerId, taskId);
    }

    public void stopBundleCheck(Player player) {
        UUID playerId = player.getUniqueId();
        Integer taskId = bundleCheckTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    /**
     * Очищает мешок от особых предметов расы.
     * Возвращает исходный ItemStack, если изменений не было, иначе новый.
     */
    private ItemStack cleanBundle(ItemStack bundle, Player player) {
        if (!(bundle.getItemMeta() instanceof BundleMeta meta)) return bundle;

        boolean changed = false;
        List<ItemStack> newContents = new ArrayList<>();

        for (ItemStack content : meta.getItems()) {
            if (content == null) continue;
            if (isCustomRaceItem(content)) {
                changed = true;
                // Возвращаем особый предмет в свободный слот инвентаря
                returnItemToInventory(player, content.clone());
                player.sendMessage("§cПредмет расы нельзя класть в мешок — он возвращён в инвентарь!");
                continue;
            }
            newContents.add(content);
        }

        if (!changed) return bundle;

        ItemStack newBundle = bundle.clone();
        BundleMeta newMeta = (BundleMeta) newBundle.getItemMeta();
        newMeta.setItems(newContents);
        newBundle.setItemMeta(newMeta);
        return newBundle;
    }

    /**
     * Возвращает предмет в первый свободный слот основного инвентаря игрока.
     * Если мест нет — бросает на землю рядом с игроком.
     */
    private void returnItemToInventory(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
        player.updateInventory();
    }

    // ===================== ОБРАБОТЧИКИ СОБЫТИЙ =====================

    // 0. Защита от автонадевания брони диспенсером (обход всех остальных проверок)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispenseArmor(BlockDispenseArmorEvent event) {
        LivingEntity target = event.getTargetEntity();
        if (!(target instanceof Player player)) return;

        if (isArmor(event.getItem()) && !canWearArmor(player)) {
            event.setCancelled(true);
        }
    }

    // 1. Защита от помещения особых предметов в рамку
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemFrameChange(PlayerItemFrameChangeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemStack();
        if (item == null) return;

        if (isCustomRaceItem(item)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете поместить этот предмет в рамку!");
            player.updateInventory();
        }
    }

    // 2. Защита от передачи особого предмета другому игроку через ПКМ
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player player = event.getPlayer();

        ItemStack item = player.getInventory().getItemInMainHand();
        if (isCustomRaceItem(item)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете передать этот предмет другому игроку!");
            player.updateInventory();
        }
    }

    // 2b. Защита от передачи особого предмета через подставку для брони (обход п.2)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();

        if (isCustomRaceItem(event.getPlayerItem())) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете поместить этот предмет на подставку для брони!");
            player.updateInventory();
        }
    }

    // 3. Защита от поднятия особых предметов с земли (для всех игроков)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        ItemStack item = event.getItem().getItemStack();
        if (isCustomRaceItem(item)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете поднять этот предмет!");
            return;
        }
        if (isForbiddenForPlayer(player, item)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете поднять этот предмет!");
        }
    }

    // 4. Защита от выброса особых предметов
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isCustomRaceItem(dropped)) {
            event.setCancelled(true);
            // Предмет автоматически возвращается в инвентарь при отмене события
            player.updateInventory();
            return;
        }
        if (isForbiddenForPlayer(player, dropped)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете выбросить этот предмет!");
        }
    }

    // 5. Защита при клике в инвентаре (включая шалкеры, сундуки, торговлю и т.д.)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        InventoryAction action = event.getAction();

        // ── Особые предметы расы ──────────────────────────────────────────────
        boolean currentIsRace = current != null && isCustomRaceItem(current);
        boolean cursorIsRace  = cursor  != null && isCustomRaceItem(cursor);

        if (currentIsRace || cursorIsRace) {
            InventoryType topType     = event.getInventory().getType();
            InventoryType clickedType = event.getClickedInventory() != null
                    ? event.getClickedInventory().getType() : null;

            boolean insidePlayerInv =
                    (clickedType == InventoryType.PLAYER || clickedType == InventoryType.CRAFTING) &&
                    (topType     == InventoryType.PLAYER || topType     == InventoryType.CRAFTING);

            // SHIFT+клик всегда пытается переместить в верхний инвентарь — запрещаем
            boolean isShiftMove = action == InventoryAction.MOVE_TO_OTHER_INVENTORY;

            // DROP / DROP_ALL — перетаскивание за край окна или Q-выброс
            boolean isDrop = action == InventoryAction.DROP_ALL_CURSOR    ||
                             action == InventoryAction.DROP_ONE_CURSOR     ||
                             action == InventoryAction.DROP_ALL_SLOT       ||
                             action == InventoryAction.DROP_ONE_SLOT;

            if (isDrop) {
                // Отменяем выброс и возвращаем предмет явно
                event.setCancelled(true);
                // Предмет, который был бы выброшен, находится либо в слоте, либо на курсоре
                ItemStack toReturn = cursorIsRace ? cursor.clone() : current.clone();
                new BukkitRunnable() {
                    @Override public void run() {
                        if (player.isOnline()) returnItemToInventory(player, toReturn);
                    }
                }.runTask(plugin);
                return;
            }

            if (isShiftMove || !insidePlayerInv) {
                // Отменяем — предмет остаётся на месте (Bukkit не двигал его ещё)
                event.setCancelled(true);
                player.updateInventory();
                return;
            }
        }

        // ── Броня для рас, которые не могут её носить ─────────────────────────
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (cursor != null && isArmor(cursor) && !canWearArmor(player)) {
                event.setCancelled(true);
                player.sendMessage("§cВаша раса не может носить броню!");
                player.updateInventory();
                return;
            }
            if (current != null && isArmor(current) && !canWearArmor(player)) {
                event.setCancelled(true);
                player.sendMessage("§cВаша раса не может носить броню!");
                player.updateInventory();
                return;
            }
        }

        // ── Тотемы и щиты ─────────────────────────────────────────────────────
        if ((current != null && (isTotem(current) || isShield(current))) ||
                (cursor != null && (isTotem(cursor) || isShield(cursor)))) {

            if ((isTotem(current) || isTotem(cursor)) && !canUseTotem(player)) {
                event.setCancelled(true);
                player.sendMessage("§cВаша раса не может использовать тотемы!");
                player.updateInventory();
                return;
            }
            if ((isShield(current) || isShield(cursor)) && !canUseShield(player)) {
                event.setCancelled(true);
                player.sendMessage("§cВаша раса не может использовать щиты!");
                player.updateInventory();
                return;
            }
        }
    }

    // 6. Защита при перетаскивании (drag) в инвентаре
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack dragged = event.getOldCursor();
        if (dragged == null || !isCustomRaceItem(dragged)) return;

        // Разрешены только слоты главного инвентаря игрока (0–35) и слоты брони/оффхенд (36–40).
        // Если хоть один слот назначения выходит за эти рамки — отменяем всё drag-действие.
        for (int slot : event.getRawSlots()) {
            if (slot < 0 || slot > 40) {
                event.setCancelled(true);
                player.updateInventory();
                return;
            }
        }
    }

    // 7. Защита при смерти — запрещённые предметы не выпадают
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isForbiddenForPlayer(player, item) || isCustomRaceItem(item)) {
                iterator.remove();
            }
        }
    }

    // 8. Дополнительные обработчики для перехвата попыток использовать запрещённые предметы
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        // Если игрок переключился на запрещённый предмет, удаляем его (это защита от использования)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    removeForbiddenItems(player);
                }
            }
        }.runTask(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Отменяем автонадевание брони правым кликом, не дожидаясь removeForbiddenItems
        Action action = event.getAction();
        ItemStack item = event.getItem();
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && isArmor(item) && !canWearArmor(player)) {
            event.setCancelled(true);
            player.sendMessage("§cВаша раса не может носить броню!");
        }

        // Если игрок пытается использовать запрещённый предмет, удаляем его
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    removeForbiddenItems(player);
                }
            }
        }.runTask(plugin);
    }

    // 9. Защита от передачи особого предмета и тотема/щита через F (смена рук)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        if (isCustomRaceItem(mainHand) || isCustomRaceItem(offHand)) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        if ((isTotem(mainHand) || isTotem(offHand)) && !canUseTotem(player)) {
            event.setCancelled(true);
            player.sendMessage("§cВаша раса не может использовать тотемы!");
            player.updateInventory();
            return;
        }

        if ((isShield(mainHand) || isShield(offHand)) && !canUseShield(player)) {
            event.setCancelled(true);
            player.sendMessage("§cВаша раса не может использовать щиты!");
            player.updateInventory();
        }
    }

    // 10. Защита от перемещения через creative-режим (средняя кнопка / pick block)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack cursor = event.getCursor();
        if (isCustomRaceItem(cursor)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    // 11. Защита от перемещения особых предметов хопперами/диспенсерами из инвентаря игрока
    // (InventoryMoveItemEvent срабатывает между двумя Inventory, не у игрока — но на всякий случай)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (isCustomRaceItem(item)) {
            event.setCancelled(true);
        }
    }
}