package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.schematic.Schematic;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class UsageHandler implements InvalidUsageHandler<CommandSender> {
    @Override
    public void handle(
            Invocation<CommandSender> invocation,
            InvalidUsage<CommandSender> result,
            ResultHandlerChain<CommandSender> chain
    ) {
        CommandSender sender = invocation.sender();
        Schematic schematic = result.getSchematic();

        if (schematic.isOnlyFirst()) {
            sender.sendMessage(String.format(
                    "%sInvalid usage of command! %s(%s)",
                    ChatColor.RED,
                    ChatColor.GRAY,
                    schematic.first()
            ));
            return;
        }

        sender.sendMessage(String.format("%sInvalid usage of command!", ChatColor.RED));
        for (String scheme : schematic.all()) {
            sender.sendMessage(String.format(
                    "%s - %s%s",
                    ChatColor.DARK_GRAY,
                    ChatColor.GRAY,
                    scheme
            ));
        }
    }
}
