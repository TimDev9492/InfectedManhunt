package me.timwastaken.infectedmanhunt.gamelogic.settings;

import java.util.HashMap;
import java.util.Map;

public class SettingsRegistry {
    private final Map<String, Object> settings;

    public static SettingsRegistry getDefaultRegistry() {
        return new SettingsRegistry();
    }

    private SettingsRegistry() {
        this.settings = new HashMap<>();
    }

    private SettingsRegistry(Map<String, Object> settings) {
        this.settings = new HashMap<>(settings);
    }

    public <T> void set(GameSetting<T> setting, T value) {
        settings.put(setting.identifier(), setting.type().cast(value));
    }

    public <T> T get(GameSetting<T> setting) {
        Object value = settings.get(setting.identifier());
        if (value == null) return setting.fallback();
        return setting.type().cast(value);
    }
}
