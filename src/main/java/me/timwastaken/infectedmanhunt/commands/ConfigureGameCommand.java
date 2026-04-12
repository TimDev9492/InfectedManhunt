package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.RootCommand;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.quoted.Quoted;
import me.timwastaken.infectedmanhunt.Game;
import me.timwastaken.infectedmanhunt.InfectedManhunt;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.exceptions.InfectedManhuntException;
import me.timwastaken.infectedmanhunt.gamelogic.settings.SettingsRegistry;
import me.timwastaken.infectedmanhunt.serialization.ConfigUtils;
import me.timwastaken.infectedmanhunt.ui.Notifications;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RootCommand
public class ConfigureGameCommand {
    private final PluginResourceManager resourceManager;
    private final ConfigUtils presetLoader;

    public ConfigureGameCommand(PluginResourceManager resourceManager, ConfigUtils presetLoader) {
        this.resourceManager = resourceManager;
        this.presetLoader = presetLoader;
    }

    @Execute(name = "settings list")
    public void listSettings(@Context CommandSender sender, @Arg Optional<ConfigurationSection> preset) {
        if (preset.isPresent()) {
            Notifications.listGameSettings(sender, SettingsRegistry.fromConfig(preset.get()));
            return;
        }
        if (!resourceManager.hasGameStarted()) {
            Notifications.errorGameIsNotRunning(sender);
            return;
        }
        Notifications.listGameSettings(sender, resourceManager.getActiveGame().getSettings());
    }

    @Execute(name = "settings list-presets")
    public void listPresets(@Context CommandSender sender) {
        List<String> presetLines = presetLoader.listPresets().stream().map(presetName -> String.format(
                Notifications.getPresetDescriptionLine(
                        presetName,
                        presetLoader.getConfig(presetName).getString("preset_description")
                ))
        ).toList();
        Notifications.sendPresetList(sender, presetLines);
    }

    @Execute(name = "settings save")
    public void savePreset(@Context CommandSender sender, @Arg String presetName, @Arg @Quoted String description) {
        if (!resourceManager.hasGameStarted()) {
            Notifications.errorGameIsNotRunning(sender);
            return;
        }
        try {
            FileConfiguration config = presetLoader.newPreset(presetName, description);
            if (config == null) {
                Notifications.errorSavingPreset(sender, presetName);
                return;
            }
            resourceManager.getActiveGame().getSettings().saveTo(config);
            presetLoader.savePreset(presetName, config);
        } catch (InfectedManhuntException exception) {
            Notifications.errorSavingPreset(sender, presetName);
        } catch (RuntimeException exception) {
            Notifications.errorChat(sender, exception.getMessage());
        }
        Notifications.sendPresetSaved(sender, presetName);
    }

    @Execute(name = "settings reload")
    public void reloadPresets(@Context CommandSender sender) {
        presetLoader.reloadPresets();
        Notifications.sendPresetsReloaded(sender);
    }
}
