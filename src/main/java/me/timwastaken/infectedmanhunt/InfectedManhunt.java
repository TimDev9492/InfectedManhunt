package me.timwastaken.infectedmanhunt;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import me.timwastaken.infectedmanhunt.commands.GameCommand;
import me.timwastaken.infectedmanhunt.commands.TrackerCommand;
import me.timwastaken.infectedmanhunt.commands.UsageHandler;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfectedManhunt extends JavaPlugin {
    private static InfectedManhunt self;

    private PluginResourceManager resourceManager;
    private LiteCommands<CommandSender> liteCommands;

    @Override
    public void onEnable() {
        // Plugin startup logic
        self = this;
        resourceManager = new PluginResourceManager(this);

        liteCommands = LiteBukkitFactory.builder(this)
                .commands(
                        new GameCommand(resourceManager),
                        new TrackerCommand(resourceManager)
                ).invalidUsage(new UsageHandler()).build();
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
