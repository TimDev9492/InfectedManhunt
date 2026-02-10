package me.timwastaken.infectedmanhunt.gamelogic.settings;

import me.timwastaken.infectedmanhunt.gamelogic.wincondition.WinCondition;
import org.bukkit.configuration.ConfigurationSection;

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

    public Map<String, Object> getSettings() {
        return Map.copyOf(settings);
    }

    public void saveTo(ConfigurationSection section) {
        for (Map.Entry<String, Object> regEntry : settings.entrySet()) {
            Object value = regEntry.getValue();
            if (value instanceof WinCondition.Registry conditionEntry) {
                section.set(regEntry.getKey(), conditionEntry.identifier());
            } else {
                section.set(regEntry.getKey(), value);
            }
        }
    }

    public static SettingsRegistry fromConfig(ConfigurationSection section) {
        Map<String, Object> loaded = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof String conditionId && WinCondition.Registry.fromIdentifier(conditionId) != null) {
                loaded.put(key, WinCondition.Registry.fromIdentifier(conditionId));
            } else {
                loaded.put(key, section.get(key));
            }
        }
        return new SettingsRegistry(loaded);
    }
}
