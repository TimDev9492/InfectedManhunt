package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import me.timwastaken.infectedmanhunt.Game;
import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.gamelogic.settings.GameSetting;
import me.timwastaken.infectedmanhunt.gamelogic.settings.SettingsRegistry;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.IPlayerTrackingStrategy;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.LazyPlayerTrackingStrategy;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.PortalEntranceTrackingStrategy;
import me.timwastaken.infectedmanhunt.gamelogic.wincondition.WinCondition;
import me.timwastaken.infectedmanhunt.ui.Notifications;
import me.timwastaken.infectedmanhunt.ui.intertory.CycleItem;
import me.timwastaken.infectedmanhunt.ui.intertory.ToggleWithActionItem;
import me.timwastaken.intertoryapi.inventories.Intertory;
import me.timwastaken.intertoryapi.inventories.IntertorySection;
import me.timwastaken.intertoryapi.inventories.items.IntertoryItem;
import me.timwastaken.intertoryapi.inventories.items.Items;
import me.timwastaken.intertoryapi.utils.IntertoryBuilder;
import me.timwastaken.intertoryapi.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@Command(name = "go")
public class GoCommand {
    private final PluginResourceManager resourceManager;

    public GoCommand(PluginResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Execute
    public void execute(@Context Player player) {
        SettingsRegistry registry = SettingsRegistry.getDefaultRegistry();

        Items.ToggleState infectRunners = new Items.ToggleState(
                Material.ZOMBIE_HEAD,
                String.format("%s%sInfection", ChatColor.DARK_RED, ChatColor.BOLD),
                "Runners become hunters upon death. The game ends when there are no runners left.",
                registry.get(GameSetting.INFECT_RUNNERS)
        );
        Items.ToggleState runnerKeepInventory = new Items.ToggleState(
                Material.ENDER_CHEST,
                String.format("%s%sRunner Keep Inventory", ChatColor.DARK_PURPLE, ChatColor.BOLD),
                "Runners keep their items upon death.",
                registry.get(GameSetting.RUNNER_KEEP_INVENTORY)
        );
        Items.ToggleState hunterKeepInventory = new Items.ToggleState(
                Material.CHEST,
                String.format("%s%sHunter Keep Inventory", ChatColor.RED, ChatColor.BOLD),
                "Hunters keep their items upon death.",
                registry.get(GameSetting.HUNTER_KEEP_INVENTORY)
        );
        Items.RangeSelect runnerLives = new Items.RangeSelect(
                Material.TOTEM_OF_UNDYING,
                String.format("%s%sRunner Lives", ChatColor.GOLD, ChatColor.BOLD),
                "The amount of deaths runners have until the game ends.",
                registry.get(GameSetting.RUNNER_LIVES),
                1,
                Integer.MAX_VALUE,
                1,
                3,
                true
        );
        List<WinCondition.Registry> conditions = List.of(WinCondition.Registry.KILL_ENDER_DRAGON);
        CycleItem winCondition = new CycleItem(
                1,
                List.of(Material.DRAGON_EGG),
                String.format("%s%sWin Condition", ChatColor.LIGHT_PURPLE, ChatColor.BOLD),
                List.of("Kill the ender dragon")
        );
        Items.RangeSelect headstartSeconds = new Items.RangeSelect(
                Material.COBWEB,
                String.format("%s%sHead Start", ChatColor.DARK_AQUA, ChatColor.BOLD),
                "The amount of seconds that hunters cannot move when the game starts, giving the runners a head start.",
                registry.get(GameSetting.RUNNER_HEADSTART_SECONDS),
                0,
                Integer.MAX_VALUE,
                5,
                30
        );
        List<IPlayerTrackingStrategy> trackingStrategies = List.of(
                new LazyPlayerTrackingStrategy(),
                new PortalEntranceTrackingStrategy()
        );
        CycleItem trackingStrategy = new CycleItem(
                2,
                List.of(Material.COMPASS, Material.RECOVERY_COMPASS),
                String.format("%s%sTracker Behavior", ChatColor.BLUE, ChatColor.BOLD),
                List.of(
                        "Simple: the tracker updates to the location of the runner when right clicked, but "
                            + "ONLY if the runner and hunter are in the same dimension. Otherwise, the tracking fails.",
                        "Advanced: when right clicked, the tracker updates to either the last location of the "
                            + "runner, if they are in the same dimension, or to the last known location (probably a "
                            + "portal) of the runner."
                )
        );
        Items.Button cancelButton = new Items.Button(
                new ItemBuilder(Material.TNT)
                        .name(String.format("%s%sCancel", ChatColor.RED, ChatColor.BOLD))
                        .build(),
                () -> {
                    player.closeInventory();;
                    player.sendMessage(String.format(
                            "%sGame configuration canceled.",
                            ChatColor.RED
                    ));
                    // whether the action was successful
                    return false;
                }
        );
        IntertorySection upper = new IntertoryBuilder(9, 3)
                .withItem(1, 1, infectRunners)
                .withItem(2, 1, runnerKeepInventory)
                .withItem(3, 1, hunterKeepInventory)
                .withItem(4, 1, runnerLives)
                .withItem(5, 1, winCondition)
                .withItem(6, 1, headstartSeconds)
                .withItem(7, 1, trackingStrategy)
//                .withItem(8, 0, cancelButton)
//                .withItem(8, 3, confirmButton)
                .withBackground(Material.GRAY_STAINED_GLASS_PANE)
                .getSection();
        IntertoryBuilder lowerBuilder = new IntertoryBuilder(9, 3);
        List<IntertoryItem> heads = new ArrayList<>();
        List<IntertoryItem> toggles = new ArrayList<>();
        Set<OptionalOnlinePlayer> runners = new HashSet<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            OptionalOnlinePlayer p = OptionalOnlinePlayer.of(online);
            runners.add(p);
            heads.add(new Items.Placeholder(
                    new ItemBuilder(Material.PLAYER_HEAD)
                            .name(String.format("%s%s", ChatColor.GREEN, online.getName()))
                            .build()
            ));
            toggles.add(new ToggleWithActionItem(
                    Material.GREEN_STAINED_GLASS_PANE,
                    Material.RED_STAINED_GLASS_PANE,
                    Notifications.getRunnerTeamName(),
                    Notifications.getHunterTeamName(),
                    "Click to change",
                    "Click to change",
                    () -> runners.add(p),
                    () -> runners.remove(p),
                    true
            ));
        }
        for (int i = 0; i < heads.size(); i++) {
            IntertoryItem headItem = heads.get(i);
            IntertoryItem toggleItem = toggles.get(i);
            lowerBuilder
                    .withItem(1 + i, 0, headItem)
                    .withItem(1 + i, 1, toggleItem);
        }
        Items.Button confirmButton = new Items.Button(
                new ItemBuilder(Material.TIPPED_ARROW)
                        .name(String.format(
                                "%s%sStart",
                                ChatColor.GREEN,
                                ChatColor.BOLD
                        ))
                        .build(),
                () -> {
                    Game.Builder builder = new Game.Builder(resourceManager);

                    player.closeInventory();
                    registry.set(GameSetting.INFECT_RUNNERS, infectRunners.getState());
                    registry.set(GameSetting.RUNNER_KEEP_INVENTORY, runnerKeepInventory.getState());
                    registry.set(GameSetting.HUNTER_KEEP_INVENTORY, hunterKeepInventory.getState());
                    registry.set(GameSetting.RUNNER_LIVES, runnerLives.getState());
                    registry.set(GameSetting.RUNNER_WIN_CONDITION, conditions.get(winCondition.getState()));
                    registry.set(GameSetting.RUNNER_HEADSTART_SECONDS, headstartSeconds.getValue());
                    builder.setTrackingStrategy(trackingStrategies.get(trackingStrategy.getState()));
                    builder.setWorld(player.getWorld());
                    builder.setSettings(registry);
                    Set<OptionalOnlinePlayer> hunters = Bukkit.getOnlinePlayers()
                            .stream().map(OptionalOnlinePlayer::of).collect(Collectors.toSet());
                    hunters.removeAll(runners);
                    builder.setRunners(runners);
                    builder.setHunters(hunters);

                    resourceManager.setActiveGame(builder.build());
                    return true;
                }
        );
        IntertorySection lower = lowerBuilder
                .withItem(0, 2, cancelButton)
                .withItem(8, 2, confirmButton)
                .withBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .getSection();
        Intertory configIntertory = new IntertoryBuilder(9, 6)
                .addSection(0, 0, upper)
                .addSection(0, 3, lower)
                .getIntertory(String.format(
                        "%sConfigure game settings",
                        ChatColor.DARK_GRAY
                ));
        configIntertory.openFor(player);
    }
}
