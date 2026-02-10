package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.RootCommand;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import me.timwastaken.infectedmanhunt.Game;
import me.timwastaken.infectedmanhunt.gamelogic.settings.SettingsRegistry;
import me.timwastaken.infectedmanhunt.serialization.ConfigUtils;
import me.timwastaken.infectedmanhunt.ui.Notifications;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

@RootCommand
public class ConfigureGameCommand {
    private final Game.Builder builder;
    private final ConfigUtils presetLoader;

    public ConfigureGameCommand(Game.Builder builder, ConfigUtils presetLoader) {
        this.builder = builder;
        this.presetLoader = presetLoader;
    }

    @Execute(name = "settings list")
    public void listSettings(@Context CommandSender sender) {
        Notifications.listGameSettings(sender, builder.getSettings());
    }

    @Execute(name = "settings load")
    void loadPreset(@Context CommandSender sender, @Arg ConfigurationSection preset) {
        builder.setSettings(SettingsRegistry.fromConfig(preset));
        Notifications.sendPresetLoaded(sender, preset.getString("preset_description"));
    }

    @Execute(name = "settings reload")
    void reloadPresets(@Context CommandSender sender) {
        presetLoader.reloadPresets();
        Notifications.sendPresetsReloaded(sender);
    }
}
