package me.timwastaken.infectedmanhunt.ui;

import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Notifications {
    private static final String PREFIX = String.format(
            "[%sInfectedManhunt%s] ",
            ChatColor.LIGHT_PURPLE,     // replace this with your plugin's color
            ChatColor.RESET
    );

    public static void sendTrackingError(OptionalOnlinePlayer target, String error) {
        sendTrackingNotification(target, asError(error));
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

    private static String withPrefix(String message) {
        return String.format("%s%s", PREFIX, message);
    }

    public static void sendTrackingNotification(OptionalOnlinePlayer target, String message) {
        target.run(p -> {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        });
    }

    public static String getHunterTeamName() {
        return String.format("%s%sHunters", ChatColor.RED, ChatColor.BOLD);
    }

    public static ChatColor getHunterTeamColor() {
        return ChatColor.RED;
    }

    public static String getHunterTeamPrefix(boolean infected) {
        return String.format(
                "%s%s%s %s| ",
                ChatColor.DARK_RED,
                ChatColor.BOLD,
                infected ? "Infected" : "Hunter",
                ChatColor.GRAY
        );
    }

    public static String getRunnerTeamName() {
        return String.format("%s%sRunners", ChatColor.BLUE, ChatColor.BOLD);
    }

    public static ChatColor getRunnerTeamColor() {
        return ChatColor.BLUE;
    }

    public static String getRunnerTeamPrefix() {
        return String.format(
                "%s%sRunner %s| ",
                ChatColor.DARK_BLUE,
                ChatColor.BOLD,
                ChatColor.GRAY
        );
    }

    public static void announceInfection(Iterable<OptionalOnlinePlayer> targets, OptionalOnlinePlayer died, Player killer) {
        String message = killer == null ? String.format(
                "%s%s%s %swas infected",
                ChatColor.DARK_RED,
                ChatColor.BOLD,
                died.getName(),
                ChatColor.RED
        ) : String.format(
                "%s%s%s %swas infected by %s%s%s",
                ChatColor.DARK_RED,
                ChatColor.BOLD,
                died.getName(),
                ChatColor.RED,
                ChatColor.DARK_RED,
                ChatColor.BOLD,
                killer.getName()
        );
        for (OptionalOnlinePlayer target : targets) {
            target.run(p -> p.sendMessage(withPrefix(message)));
        }
    }

    public static void announceRunnerDeath(Iterable<OptionalOnlinePlayer> targets, OptionalOnlinePlayer died, Player killer, int runnerLivesLeft) {
        String message = killer == null ? String.format(
                "%s%s%s %sdied",
                ChatColor.DARK_RED,
                ChatColor.BOLD,
                died.getName(),
                ChatColor.RED
        ) : String.format(
                "%s%s%s %swas killed by %s%s%s",
                ChatColor.DARK_RED,
                ChatColor.BOLD,
                died.getName(),
                ChatColor.RED,
                ChatColor.DARK_RED,
                ChatColor.BOLD,
                killer.getName()
        );
        for (OptionalOnlinePlayer target : targets) {
            target.run(p -> {
                p.sendMessage(withPrefix(message));
                if (runnerLivesLeft > 0)
                    p.sendMessage(withPrefix(String.format(
                            "%sRunners have %s%s%d %s%slives left",
                            ChatColor.GRAY,
                            ChatColor.YELLOW,
                            ChatColor.BOLD,
                            runnerLivesLeft,
                            ChatColor.RESET,
                            ChatColor.GRAY
                    )));
            });
        }
    }

    public static void announceRunnersWin(Iterable<OptionalOnlinePlayer> targets) {
        for (OptionalOnlinePlayer target : targets) {
            target.run(p -> p.sendTitle(
                    String.format(getRunnerTeamName()),
                    String.format("%swon the game", ChatColor.GRAY),
                    10, 80, 10
            ));
        }
    }

    public static void announceHuntersWin(Iterable<OptionalOnlinePlayer> targets) {
        for (OptionalOnlinePlayer target : targets) {
            target.run(p -> p.sendTitle(
                    String.format(getHunterTeamName()),
                    String.format("%swon the game", ChatColor.GRAY),
                    10, 80, 10
            ));
        }
    }
}
