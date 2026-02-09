package me.timwastaken.infectedmanhunt.ui;

import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Notifications {
    private static final String PREFIX = String.format(
            "[%sInfectedManhunt%s] ",
            ChatColor.LIGHT_PURPLE,     // replace this with your plugin's color
            ChatColor.RESET
    );

    public static void sendTrackingError(OptionalOnlinePlayer target, String error) {
        sendTrackingNotification(target, asError(error));
    }

    public static void sendChat(OptionalOnlinePlayer target, String message) {
        target.run(p -> p.sendMessage(message));
    }

    public static String getTrackerDisplayName() {
        return String.format("%s%sTracker", ChatColor.DARK_PURPLE, ChatColor.BOLD);
    }

    public static void errorChat(CommandSender target, String error) {
        target.sendMessage(String.format("%s%s", PREFIX, asError(error)));
    }

    public static void errorGameIsRunning(CommandSender target) {
        errorChat(target, "The game is already running.");
    }

    private static String asError(String error) {
        return String.format("%s%s", ChatColor.RED, error);
    }

    public static void sendTrackingNotification(OptionalOnlinePlayer target, String message) {
        target.run(p -> {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        });
    }

    public static String getInfectedTeamName() {
        return String.format("%s%sInfected", ChatColor.RED, ChatColor.BOLD);
    }

    public static ChatColor getInfectedTeamColor() {
        return ChatColor.RED;
    }

    public static String getInfectedTeamPrefix() {
        return String.format("%s%s", ChatColor.RED, ChatColor.BOLD);
    }
}
