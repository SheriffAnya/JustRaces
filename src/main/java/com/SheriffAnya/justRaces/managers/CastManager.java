package com.SheriffAnya.justRaces.managers;

import com.SheriffAnya.justRaces.JustRaces;
import com.SheriffAnya.justRaces.objects.Cast;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import java.util.*;

public class CastManager {

    private final JustRaces plugin;
    private final Map<String, Cast> casts = new HashMap<>();
    private final Map<String, String> playerCasts = new HashMap<>(); // playerName -> castName
    private boolean debug;

    public CastManager(JustRaces plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    public void initialize() {
        createDefaultCasts();
    }

    public void loadConfig() {
        casts.clear();
        createDefaultCasts();
    }

    private void createDefaultCasts() {
        String[] castNames = plugin.getConfigManager().getCastNames();

        for (String castName : castNames) {
            if (!casts.containsKey(castName)) {
                Cast cast = new Cast(castName);

                int maxSize = plugin.getConfigManager().getCastMaxSize(castName);
                cast.setMaxSize(maxSize);

                double health = plugin.getConfigManager().getCastHealth(castName);
                cast.setHealth(health);

                float size = plugin.getConfigManager().getCastSize(castName);
                cast.setSize(size);

                String color = plugin.getConfigManager().getCastColor(castName);
                cast.setColor(color);

                boolean canBreatheUnderwater = plugin.getConfigManager().canBreatheUnderwater(castName);
                boolean needsWaterToBreathe = plugin.getConfigManager().needsWaterToBreathe(castName);
                float underwaterMiningSpeed = plugin.getConfigManager().getUnderwaterMiningSpeed(castName);

                cast.setCanBreatheUnderwater(canBreatheUnderwater);
                cast.setNeedsWaterToBreathe(needsWaterToBreathe);
                cast.setUnderwaterMiningSpeed(underwaterMiningSpeed);

                casts.put(castName, cast);
            }
        }

        if (debug) plugin.getLogger().info("Создано рас: " + casts.size());
    }

    public void loadPlayerCasts() {
        playerCasts.clear();
        plugin.getDataManager().loadCasts();
    }

    public void addCast(Cast cast) {
        casts.put(cast.getName(), cast);
    }

    public boolean assignCast(Player player) {
        String playerName = player.getName();

        if (playerCasts.containsKey(playerName)) {
            return false;
        }

        List<Cast> availableCasts = new ArrayList<>();
        for (Cast cast : casts.values()) {
            if (!cast.isFull() && !plugin.getConfigManager().isSoloCast(cast.getName())) {
                availableCasts.add(cast);
            }
        }

        if (availableCasts.isEmpty()) {
            return false;
        }

        Random random = new Random();
        Cast selectedCast = availableCasts.get(random.nextInt(availableCasts.size()));

        if (selectedCast.addMember(playerName)) {
            playerCasts.put(playerName, selectedCast.getName());
            plugin.getDataManager().saveCastsAsync();
            applyCastEffects(player, selectedCast);
            if (debug) plugin.getLogger().info("Игроку " + playerName + " назначена раса " + selectedCast.getName());
            return true;
        }

        return false;
    }

    public void applyCastEffects(Player player, Cast cast) {
        if (cast == null || player == null) return;

        String castName = cast.getName();
        removeCastEffects(player);

        switch (castName) {
            case "Русал":
                applyRusalEffects(player);
                break;
            case "Эндермен":
                applyEnderEffects(player);
                break;
            case "Нежить":
                applyUndeadEffects(player);
                break;
            case "Элей":
                applyEleiEffects(player);
                break;
            case "Тень":
                applyShadowEffects(player);
                break;
            case "Эфрит":
                applyEffritEffects(player);
                break;
            case "Гигант":
                applyGiantEffects(player);
                break;
            case "Фортуна":
                applyFortuneEffects(player);
                break;
            case "Голем":
                applyGolemEffects(player);
                break;
            case "Варден":
                applyWardenEffects(player);
                break;
            case "Друид":
                applyDruidEffects(player);
                break;
            case "ХранительРун":
                applyRuneKeeperEffects(player);
                break;
            default:
                applyBaseCastEffects(player, cast);
                break;
        }
        plugin.getArmorManager().startBundleCheck(player);
    }

    private void applyRusalEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Русал");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Русал");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getRusalCast().applyRusalEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Русала: " + e.getMessage());
        }
    }

    private void applyEnderEffects(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize("Эндермен");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            double health = plugin.getConfigManager().getCastHealth("Эндермен");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            double attackRange = plugin.getConfigManager().getEnderAttackRange();
            AttributeInstance attackRangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
            if (attackRangeAttr != null) {
                attackRangeAttr.setBaseValue(attackRange);
            }

            plugin.getEnderCast().applyEnderEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Эндермена: " + e.getMessage());
        }
    }

    private void applyUndeadEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Нежить");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Нежить");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getUndeadCast().applyUndeadEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Нежити: " + e.getMessage());
        }
    }

    private void applyEleiEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Элей");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Элей");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getEleiCast().applyEleiEffects(player);

            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Элей: " + e.getMessage());
        }
    }

    private void applyShadowEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Тень");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Тень");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getShadowCast().applyShadowEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Тени: " + e.getMessage());
        }
    }

    private void applyEffritEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Эфрит");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Эфрит");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getEffritCast().applyEffritEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Эфрита: " + e.getMessage());
        }
    }

    private void applyGiantEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Гигант");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Гигант");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getGiantCast().applyGiantEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Гиганта: " + e.getMessage());
        }
    }

    private void applyFortuneEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Фортуна");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Фортуна");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getFortuneCast().applyFortuneEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Фортуны: " + e.getMessage());
        }
    }

    private void applyGolemEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Голем");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Голем");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getGolemCast().applyGolemEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Голема: " + e.getMessage());
        }
    }

    private void applyWardenEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Варден");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Варден");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getWardenCast().applyWardenEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Вардена: " + e.getMessage());
        }
    }

    private void applyDruidEffects(Player player) {
        try {
            double health = plugin.getConfigManager().getCastHealth("Друид");
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = plugin.getConfigManager().getCastSize("Друид");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getDruidCast().applyDruidEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Друида: " + e.getMessage());
        }
    }

    private void applyRuneKeeperEffects(Player player) {
        try {
            float size = plugin.getConfigManager().getCastSize("ХранительРун");
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }

            plugin.getRuneKeeperCast().applyRuneKeeperEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении эффектов Хранителя Рун: " + e.getMessage());
        }
    }

    private void applyBaseCastEffects(Player player, Cast cast) {
        try {
            double health = cast.getHealth();
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(health);
                player.setHealth(Math.min(health, player.getHealth()));
            }

            float size = cast.getSize();
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(size);
            }
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при применении базовых эффектов расы: " + e.getMessage());
        }
    }

    public void removeCastEffects(Player player) {
        if (player == null) return;

        String playerName = player.getName();
        String castName = getPlayerCastName(playerName);
        if (castName == null) return;

        switch (castName) {
            case "Русал":
                removeRusalEffects(player);
                break;
            case "Эндермен":
                removeEnderEffects(player);
                break;
            case "Нежить":
                removeUndeadEffects(player);
                break;
            case "Элей":
                removeEleiEffects(player);
                break;
            case "Тень":
                removeShadowEffects(player);
                break;
            case "Эфрит":
                removeEffritEffects(player);
                break;
            case "Гигант":
                removeGiantEffects(player);
                break;
            case "Фортуна":
                removeFortuneEffects(player);
                break;
            case "Голем":
                removeGolemEffects(player);
                break;
            case "Варден":
                removeWardenEffects(player);
                break;
            case "Друид":
                removeDruidEffects(player);
                break;
            case "ХранительРун":
                removeRuneKeeperEffects(player);
                break;
            default:
                removeBaseCastEffects(player);
                break;
        }
        plugin.getArmorManager().stopBundleCheck(player);
    }

    private void removeRusalEffects(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            plugin.getRusalCast().removeRusalEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Русала: " + e.getMessage());
        }
    }

    private void removeEnderEffects(Player player) {
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance attackRangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
            if (attackRangeAttr != null) {
                attackRangeAttr.setBaseValue(3.0);
            }

            plugin.getEnderCast().removeEnderEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Эндермена: " + e.getMessage());
        }
    }

    private void removeUndeadEffects(Player player) {
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            plugin.getUndeadCast().removeUndeadEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Нежити: " + e.getMessage());
        }
    }

    private void removeEleiEffects(Player player) {
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            plugin.getEleiCast().removeEleiEffects(player);

            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Элей: " + e.getMessage());
        }
    }

    private void removeShadowEffects(Player player) {
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            plugin.getShadowCast().removeShadowEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Тени: " + e.getMessage());
        }
    }

    private void removeEffritEffects(Player player) {
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            plugin.getEffritCast().removeEffritEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Эфрита: " + e.getMessage());
        }
    }

    private void removeGiantEffects(Player player) {
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            plugin.getGiantCast().removeGiantEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Гиганта: " + e.getMessage());
        }
    }

    private void removeFortuneEffects(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            plugin.getFortuneCast().removeFortuneEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Фортуны: " + e.getMessage());
        }
    }

    private void removeGolemEffects(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            plugin.getGolemCast().removeGolemEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Голема: " + e.getMessage());
        }
    }

    private void removeWardenEffects(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            plugin.getWardenCast().removeWardenEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Вардена: " + e.getMessage());
        }
    }

    private void removeDruidEffects(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            plugin.getDruidCast().removeDruidEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Друида: " + e.getMessage());
        }
    }

    private void removeRuneKeeperEffects(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }

            plugin.getRuneKeeperCast().removeRuneKeeperEffects(player);
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении эффектов Хранителя Рун: " + e.getMessage());
        }
    }

    private void removeBaseCastEffects(Player player) {
        try {
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double currentPercent = player.getHealth() / maxHealthAttr.getBaseValue();
                maxHealthAttr.setBaseValue(20.0);
                player.setHealth(20.0 * currentPercent);
            }

            AttributeInstance scaleAttr = player.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("Ошибка при удалении базовых эффектов расы: " + e.getMessage());
        }
    }

    public Cast getCast(String castName) {
        return casts.get(castName);
    }

    public Cast getPlayerCast(String playerName) {
        String castName = playerCasts.get(playerName);
        return castName != null ? casts.get(castName) : null;
    }

    public String getPlayerCastName(String playerName) {
        return playerCasts.get(playerName);
    }

    public boolean isInCast(String playerName) {
        return playerCasts.containsKey(playerName);
    }

    public Map<String, Cast> getCasts() {
        return Collections.unmodifiableMap(casts);
    }

    public Map<String, String> getPlayerCastsMap() {
        return playerCasts;
    }

    public void setPlayerCast(String playerName, String castName) {
        Player player = Bukkit.getPlayer(playerName);
        Cast cast = getCast(castName);

        if (cast == null) {
            if (debug) plugin.getLogger().warning("Раса '" + castName + "' не найдена");
            return;
        }

        if (plugin.getConfigManager().isSoloCast(castName) && !cast.isEmpty()) {
            String existingMember = null;
            for (String memberName : cast.getMembersNames()) {
                existingMember = memberName;
                break;
            }

            if (existingMember != null && !existingMember.equals(playerName)) {
                if (player != null) {
                    player.sendMessage("§cЭта раса уже занята игроком " + existingMember);
                }
                return;
            }
        }

        // Проверяем дублирование ДО удаления из старой расы — иначе проверка
        // hasMember() никогда не сработает, так как старая раса уже будет очищена.
        if (cast.hasMember(playerName)) {
            if (player != null) {
                player.sendMessage("§cВы уже состоите в этой расе");
            }
            return;
        }

        Cast oldCast = getPlayerCast(playerName);
        if (oldCast != null) {
            oldCast.removeMember(playerName);
            if (player != null) {
                removeCastEffects(player);
            }
        }

        if (!plugin.getConfigManager().isSoloCast(castName) && cast.isFull()) {
            if (player != null) {
                player.sendMessage("§cЭта раса уже заполнена");
            }
            // Возвращаем игрока в старую расу, так как новая недоступна
            if (oldCast != null) {
                oldCast.addMember(playerName);
                if (player != null && player.isOnline()) {
                    applyCastEffects(player, oldCast);
                }
            }
            return;
        }

        if (!cast.addMember(playerName)) {
            // Не удалось добавить - возвращаем в старую расу
            if (oldCast != null) {
                oldCast.addMember(playerName);
                if (player != null && player.isOnline()) {
                    applyCastEffects(player, oldCast);
                }
            }
            return;
        }

        playerCasts.put(playerName, castName);

        if (player != null && player.isOnline()) {
            applyCastEffects(player, cast);
            plugin.getArmorManager().removeForbiddenItems(player);
            player.sendMessage("§aВаша раса изменена на: §e" + castName);
        }

        plugin.getDataManager().saveCastsAsync();
    }

    public void forceUpdatePlayerEffects(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return;

        Cast cast = getPlayerCast(playerName);
        if (cast == null) return;

        removeCastEffects(player);
        applyCastEffects(player, cast);
    }

    public void reloadCastsFromConfig() {
        loadConfig();
        if (debug) plugin.getLogger().info("Расы перезагружены из конфигурации");
    }
}