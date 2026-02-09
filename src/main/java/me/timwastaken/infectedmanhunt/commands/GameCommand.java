package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import me.timwastaken.infectedmanhunt.Game;
import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.ContinuousTrackingStrategy;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.PortalEntranceTrackingStrategy;
import me.timwastaken.infectedmanhunt.ui.Notifications;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Command(name = "game")
public class GameCommand {
    private final PluginResourceManager resourceManager;

    public GameCommand(PluginResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Execute(name = "start")
    public void execute(@Context Player player) {
        if (resourceManager.getActiveGame() != null) {
            Notifications.errorGameIsRunning(player);
            return;
        }
        Game.Builder builder = new Game.Builder(resourceManager)
                .setTrackingStrategy(new PortalEntranceTrackingStrategy())
                .setInfected(OptionalOnlinePlayer.of(player));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) builder.setRunners(OptionalOnlinePlayer.of(online));
        }
        resourceManager.setActiveGame(builder.build());
    }
}
