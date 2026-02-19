package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import me.timwastaken.infectedmanhunt.Game;
import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.gamelogic.settings.GameSetting;
import me.timwastaken.infectedmanhunt.gamelogic.settings.SettingsRegistry;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.ContinuousTrackingStrategy;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
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
        List<WinCondition.Registry> conditions = List.of(
                WinCondition.Registry.KILL_ENDER_DRAGON,
                WinCondition.Registry.ADVANCEMENT_UNEASY_ALLIANCE,
                WinCondition.Registry.ADVANCEMENT_CURE_ZOMBIE_VILLAGER,
                WinCondition.Registry.ADVANCEMENT_UNLOCK_OMINOUS_VAULT,
                WinCondition.Registry.ADVANCEMENT_PLAY_MUSIC_DISC_IN_MEADOWS
        );
        CycleItem winCondition = new CycleItem(
                5,
                List.of(
                        Material.DRAGON_EGG,
                        Material.GHAST_TEAR,
                        Material.GOLDEN_APPLE,
                        Material.OMINOUS_BOTTLE,
                        Material.JUKEBOX
                ),
                String.format("%s%sWin Condition", ChatColor.LIGHT_PURPLE, ChatColor.BOLD),
                List.of(
                        "Kill the Ender Dragon",
                        "Rescue a Ghast from the Nether, bring it safely to the Overworld... and then kill it",
                        "Weaken and then cure a Zombie Villager",
                        "Unlock an Ominous Vault with an Ominous Trial Key",
                        "Play a music disc inside a jukebox in the meadows biome"
                )
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
        List<Supplier<IPlayerTrackingStrategy>> trackingStrategies = List.of(
                LazyPlayerTrackingStrategy::new,
                PortalEntranceTrackingStrategy::new,
                () -> new ContinuousTrackingStrategy(resourceManager, new PortalEntranceTrackingStrategy())
        );
        CycleItem trackingStrategy = new CycleItem(
                3,
                List.of(Material.COMPASS, Material.RECOVERY_COMPASS, Material.CALIBRATED_SCULK_SENSOR),
                String.format("%s%sTracker Behavior", ChatColor.BLUE, ChatColor.BOLD),
                List.of(
                        "Simple: the tracker updates to the location of the runner when right clicked, but "
                            + "ONLY if the runner and hunter are in the same dimension. Otherwise, the tracking fails.",
                        "Advanced: when right clicked, the tracker updates to either the last location of the "
                            + "runner, if they are in the same dimension, or to the last known location (probably a "
                            + "portal) of the runner.",
                        "Auto-Update: the tracker continuously updates to always point to the location of the runner, "
                            + "if they are in the same dimension, or to the last known location (probably a portal) "
                            + "of the runner."
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
        List<Consumer<SettingsRegistry>> configureCookedFood = List.of(
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_COOKED_FOOD, false);
                    reg.set(GameSetting.HUNTER_DROP_COOKED_FOOD, false);
                },
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_COOKED_FOOD, true);
                    reg.set(GameSetting.HUNTER_DROP_COOKED_FOOD, false);
                },
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_COOKED_FOOD, false);
                    reg.set(GameSetting.HUNTER_DROP_COOKED_FOOD, true);
                },
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_COOKED_FOOD, true);
                    reg.set(GameSetting.HUNTER_DROP_COOKED_FOOD, true);
                }
        );
        CycleItem dropCookedFood = new CycleItem(
                4,
                List.of(
                        Material.BEEF,
                        Material.COOKED_COD,
                        Material.COOKED_PORKCHOP,
                        Material.COOKED_BEEF
                ),
                String.format("%s%sDrop Cooked Food", ChatColor.GOLD, ChatColor.BOLD),
                List.of(
                        String.format("%sDisabled", ChatColor.RED),
                        String.format("%sRunners Only", Notifications.getRunnerTeamColor()),
                        String.format("%sHunters Only", Notifications.getHunterTeamColor()),
                        String.format("%sAlways", ChatColor.GREEN)
                )
        );
        List<Consumer<SettingsRegistry>> configureSmeltedOres = List.of(
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_SMELTED_ORES, false);
                    reg.set(GameSetting.HUNTER_DROP_SMELTED_ORES, false);
                },
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_SMELTED_ORES, true);
                    reg.set(GameSetting.HUNTER_DROP_SMELTED_ORES, false);
                },
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_SMELTED_ORES, false);
                    reg.set(GameSetting.HUNTER_DROP_SMELTED_ORES, true);
                },
                reg -> {
                    reg.set(GameSetting.RUNNER_DROP_SMELTED_ORES, true);
                    reg.set(GameSetting.HUNTER_DROP_SMELTED_ORES, true);
                }
        );
        CycleItem dropSmeltedOres = new CycleItem(
                4,
                List.of(
                        Material.RAW_IRON,
                        Material.IRON_INGOT,
                        Material.COPPER_INGOT,
                        Material.BLAST_FURNACE
                ),
                String.format("%s%sDrop Smelted Ores", ChatColor.GOLD, ChatColor.BOLD),
                List.of(
                        String.format("%sDisabled", ChatColor.RED),
                        String.format("%sRunners Only", Notifications.getRunnerTeamColor()),
                        String.format("%sHunters Only", Notifications.getHunterTeamColor()),
                        String.format("%sAlways", ChatColor.GREEN)
                )
        );
        Items.RangeSelect runnerMaxHealth = new Items.RangeSelect(
                Material.ENCHANTED_GOLDEN_APPLE,
                String.format("%s%sRunner Health", Notifications.getRunnerTeamColor(), ChatColor.BOLD),
                "The amount of health runners have (20HP=10×❤).",
                20,
                1,
                Integer.MAX_VALUE,
                1,
                5
        );
        Items.RangeSelect hunterMaxHealth = new Items.RangeSelect(
                Material.ENCHANTED_GOLDEN_APPLE,
                String.format("%s%sHunter Health", Notifications.getHunterTeamColor(), ChatColor.BOLD),
                "The amount of health hunters have (20HP=10×❤).",
                20,
                1,
                Integer.MAX_VALUE,
                1,
                5
        );
        IntertorySection upper = new IntertoryBuilder(9, 3)
                .withItem(1, 0, infectRunners)
                .withItem(2, 0, runnerKeepInventory)
                .withItem(3, 0, hunterKeepInventory)
                .withItem(4, 0, runnerLives)
                .withItem(5, 0, winCondition)
                .withItem(6, 0, headstartSeconds)
                .withItem(7, 0, trackingStrategy)
                .withItem(1, 1, dropCookedFood)
                .withItem(3, 1, dropSmeltedOres)
                .withItem(5, 1, runnerMaxHealth)
                .withItem(7, 1, hunterMaxHealth)
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
                    registry.set(GameSetting.RUNNER_MAX_HEALTH, runnerMaxHealth.getValue());
                    registry.set(GameSetting.HUNTER_MAX_HEALTH, hunterMaxHealth.getValue());
                    configureCookedFood.get(dropCookedFood.getState()).accept(registry);
                    configureSmeltedOres.get(dropSmeltedOres.getState()).accept(registry);
                    builder.setTrackingStrategy(trackingStrategies.get(trackingStrategy.getState()).get());
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
