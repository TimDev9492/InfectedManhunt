package me.timwastaken.infectedmanhunt.gamelogic.settings;

import me.timwastaken.infectedmanhunt.gamelogic.WinCondition;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.KillEnderDragonCondition;

public record GameSetting<T>(String identifier, T fallback, Class<T> type) {
    private static GameSetting<Integer> integer(String id, int def) {
        return new GameSetting<>(id, def, Integer.class);
    }

    private static GameSetting<Boolean> bool(String id, boolean def) {
        return new GameSetting<>(id, def, Boolean.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static GameSetting<WinCondition<?>> condition(String id, WinCondition<?> def) {
        return new GameSetting<>(id, def, (Class<WinCondition<?>>) (Class) WinCondition.class);
    }

    public static final GameSetting<Boolean> INFECT_RUNNERS = GameSetting.bool("infect_runners", false);
    public static final GameSetting<Boolean> RUNNER_KEEP_INVENTORY = GameSetting.bool("runner_keep_inv", true);
    public static final GameSetting<Boolean> HUNTER_KEEP_INVENTORY = GameSetting.bool("hunter_keep_inv", false);
    public static final GameSetting<Integer> RUNNER_LIVES = GameSetting.integer("runner_lives", 1);
    public static final GameSetting<WinCondition<?>> RUNNER_WIN_CONDITION = GameSetting.condition("win_condition", new KillEnderDragonCondition());
}
