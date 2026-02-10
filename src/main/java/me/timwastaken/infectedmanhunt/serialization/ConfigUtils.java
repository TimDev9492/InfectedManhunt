package me.timwastaken.infectedmanhunt.serialization;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigUtils {
    private static final String DESCRIPTION_KEY = "preset_description";

    private Plugin plugin;
    private List<String> cachedPresets;

    public ConfigUtils(Plugin plugin) {
        this.plugin = plugin;
        this.cachedPresets = refreshPresets();
    }

    private File presetToFile(String preset) {
        return new File(plugin.getDataFolder(), String.format("%s.yml", preset));
    }

    private boolean isPresetConfiguration(FileConfiguration fileConfiguration) {
        return fileConfiguration.contains(DESCRIPTION_KEY) && fileConfiguration.getString(DESCRIPTION_KEY) != null;
    }

    private String fileToPreset(File file) {
        return file.getName().replace(".yml", "");
    }

    public FileConfiguration getConfig(String preset) {
        File configFile = presetToFile(preset);
        if (!configFile.exists()) return null;
        FileConfiguration config = YamlConfiguration.loadConfiguration(presetToFile(preset));
        if (!isPresetConfiguration(config)) return null;
        return config;
    }

    public void savePreset(String preset, FileConfiguration configuration) {
        try {
            configuration.save(presetToFile(preset));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> refreshPresets() {
        File[] ymlFiles = plugin.getDataFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles == null) return Collections.emptyList();
        List<String> presets = new ArrayList<>();
        for (File ymlFile : ymlFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(ymlFile);
            if (isPresetConfiguration(config)) presets.add(fileToPreset(ymlFile));
        }
        return presets;
    }

    public List<String> listPresets() {
        return List.copyOf(cachedPresets);
    }

    public void reloadPresets() {
        cachedPresets = refreshPresets();
    }
}
