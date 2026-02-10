package me.timwastaken.infectedmanhunt.gamelogic.settings;

import me.timwastaken.infectedmanhunt.gamelogic.wincondition.WinCondition;
import me.timwastaken.infectedmanhunt.gamelogic.wincondition.KillEnderDragonCondition;

import java.util.List;

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

    public static final GameSetting<Boolean> INFECT_RUNNERS = GameSetting.bool("infect_runners", false);
    public static final GameSetting<Boolean> RUNNER_KEEP_INVENTORY = GameSetting.bool("runner_keep_inv", true);
    public static final GameSetting<Boolean> HUNTER_KEEP_INVENTORY = GameSetting.bool("hunter_keep_inv", false);
    public static final GameSetting<Integer> RUNNER_LIVES = GameSetting.integer("runner_lives", 1);
    public static final GameSetting<WinCondition.Registry> RUNNER_WIN_CONDITION = GameSetting.condition("win_condition", WinCondition.Registry.KILL_ENDER_DRAGON);
    public static final GameSetting<Integer> RUNNER_HEADSTART_SECONDS = GameSetting.integer("runner_headstart", 0);

    public static List<GameSetting<?>> all() {
        return List.of(
                INFECT_RUNNERS,
                RUNNER_KEEP_INVENTORY,
                HUNTER_KEEP_INVENTORY,
                RUNNER_LIVES,
                RUNNER_WIN_CONDITION,
                RUNNER_HEADSTART_SECONDS
        );
    }
}
