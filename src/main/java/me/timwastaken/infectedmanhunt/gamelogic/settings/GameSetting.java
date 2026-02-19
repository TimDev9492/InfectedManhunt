package me.timwastaken.infectedmanhunt.gamelogic.settings;

import me.timwastaken.infectedmanhunt.gamelogic.wincondition.WinCondition;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public record GameSetting<T>(String identifier, T fallback, Class<T> type) {
    private static GameSetting<Integer> integer(String id, int def) {
        return new GameSetting<>(id, def, Integer.class);
    }

    private static GameSetting<Boolean> bool(String id, boolean def) {
        return new GameSetting<>(id, def, Boolean.class);
    }

    private static GameSetting<WinCondition.Registry> condition(String id, WinCondition.Registry def) {
        return new GameSetting<>(id, def, WinCondition.Registry.class);
    }

    public static final GameSetting<Boolean> INFECT_RUNNERS = GameSetting.bool("infect_runners", true);
    public static final GameSetting<Boolean> RUNNER_KEEP_INVENTORY = GameSetting.bool("runner_keep_inv", true);
    public static final GameSetting<Boolean> HUNTER_KEEP_INVENTORY = GameSetting.bool("hunter_keep_inv", false);
    public static final GameSetting<Integer> RUNNER_LIVES = GameSetting.integer("runner_lives", 1);
    public static final GameSetting<WinCondition.Registry> RUNNER_WIN_CONDITION = GameSetting.condition("win_condition", WinCondition.Registry.KILL_ENDER_DRAGON);
    public static final GameSetting<Integer> RUNNER_HEADSTART_SECONDS = GameSetting.integer("runner_headstart", 0);
    public static final GameSetting<Boolean> RUNNER_DROP_COOKED_FOOD = GameSetting.bool("runner_drop_cooked_food", false);
    public static final GameSetting<Boolean> HUNTER_DROP_COOKED_FOOD = GameSetting.bool("hunter_drop_cooked_food", false);
    public static final GameSetting<Boolean> RUNNER_DROP_SMELTED_ORES = GameSetting.bool("runner_drop_smelted_ores", false);
    public static final GameSetting<Boolean> HUNTER_DROP_SMELTED_ORES = GameSetting.bool("hunter_drop_smelted_ores", false);
    public static final GameSetting<Integer> RUNNER_MAX_HEALTH = GameSetting.integer("runner_max_health", 20);
    public static final GameSetting<Integer> HUNTER_MAX_HEALTH = GameSetting.integer("hunter_max_health", 20);

    public static List<GameSetting<?>> all() {
        return List.of(
                INFECT_RUNNERS,
                RUNNER_KEEP_INVENTORY,
                HUNTER_KEEP_INVENTORY,
                RUNNER_LIVES,
                RUNNER_WIN_CONDITION,
                RUNNER_HEADSTART_SECONDS,
                RUNNER_DROP_COOKED_FOOD,
                HUNTER_DROP_COOKED_FOOD,
                RUNNER_DROP_SMELTED_ORES,
                HUNTER_DROP_SMELTED_ORES,
                RUNNER_MAX_HEALTH,
                HUNTER_MAX_HEALTH
        );
    }
}
