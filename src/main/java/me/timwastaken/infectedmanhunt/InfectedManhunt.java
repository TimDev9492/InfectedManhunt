package me.timwastaken.infectedmanhunt;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import me.timwastaken.infectedmanhunt.commands.*;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.serialization.ConfigUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfectedManhunt extends JavaPlugin {
    private static InfectedManhunt self;

    private PluginResourceManager resourceManager;
    private LiteCommands<CommandSender> liteCommands;
    private ConfigUtils presetLoader;

    @Override
    public void onEnable() {
        // Plugin startup logic
        self = this;
        resourceManager = new PluginResourceManager(this);
        presetLoader = new ConfigUtils(this);

        liteCommands = LiteBukkitFactory.builder(this)
                .commands(
                        new GameCommand(resourceManager),
                        new TrackerCommand(resourceManager),
                        new ConfigureGameCommand(new Game.Builder(resourceManager), presetLoader),
                        new GoCommand(resourceManager)
                )
                .invalidUsage(new UsageHandler())
                .argument(ConfigurationSection.class, new PresetArgument(presetLoader))
                .build();

        saveResource("default.yml", true);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        resourceManager.unregisterTasks();
        resourceManager.unregisterEvents();
        resourceManager.destroyGame();

        if (liteCommands != null) liteCommands.unregister();
    }

    public static InfectedManhunt getInstance() {
        return self;
    }
}
