package me.timwastaken.infectedmanhunt.gamelogic.settings;

import me.timwastaken.infectedmanhunt.gamelogic.tracking.PlayerTrackingStrategy;
import me.timwastaken.infectedmanhunt.gamelogic.wincondition.WinCondition;
import me.timwastaken.infectedmanhunt.serialization.RegistryEnum;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsRegistry {
    private static final List<Class<? extends Enum<? extends RegistryEnum>>> REGISTRY_CLASSES = List.of(
            WinCondition.Registry.class,
            PlayerTrackingStrategy.Registry.class
    );

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
        for (GameSetting<?> setting : GameSetting.all()) {
            Object value = get(setting);
            if (value instanceof RegistryEnum conditionEntry) {
                section.set(setting.identifier(), conditionEntry.identifier());
            } else {
                section.set(setting.identifier(), value);
            }
        }
    }

    public static SettingsRegistry fromConfig(ConfigurationSection section) {
        Map<String, Object> loaded = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof String registryKey) {
                boolean isRegistry = false;
                for (Class<? extends Enum<? extends RegistryEnum>> registryClass : REGISTRY_CLASSES) {
                    try {
                        Method regEntryFromId = registryClass.getMethod("fromIdentifier", String.class);
                        Object regEntry = regEntryFromId.invoke(null, registryKey);
                        if (regEntry == null) continue;
                        loaded.put(key, regEntry);
                        isRegistry = true;
                        break;
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        throw new IllegalStateException("Registry classes MUST include method fromIdentifier");
                    }
                }
                if (!isRegistry) loaded.put(key, registryKey);
            } else {
                loaded.put(key, section.get(key));
            }
        }
        return new SettingsRegistry(loaded);
    }
}
