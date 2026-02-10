package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import me.timwastaken.infectedmanhunt.Game;
import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.ui.Notifications;
import org.bukkit.entity.Player;

@Command(name = "tracker")
public class TrackerCommand {
    private final PluginResourceManager resourceManager;

    public TrackerCommand(PluginResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Execute
    public void execute(@Context Player player) {
        if (!resourceManager.hasGameStarted()) {
            Notifications.errorChat(player, "The game is not running. Start a game with /game start");
            return;
        }
        resourceManager.getActiveGame().giveTrackerTo(OptionalOnlinePlayer.of(player));
    }
}
