package me.timwastaken.infectedmanhunt;

import com.google.common.collect.Iterators;
import me.timwastaken.infectedmanhunt.common.ItemUtils;
import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.exceptions.InfectedManhuntException;
import me.timwastaken.infectedmanhunt.gamelogic.WinCondition;
import me.timwastaken.infectedmanhunt.gamelogic.settings.GameSetting;
import me.timwastaken.infectedmanhunt.gamelogic.settings.SettingsRegistry;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.IPlayerTrackingStrategy;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.TrackingRequest;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.TrackingResult;
import me.timwastaken.infectedmanhunt.ui.Notifications;
import me.timwastaken.infectedmanhunt.ui.Sounds;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class Game implements Listener {
    private static final boolean DEBUG = true;
    private static final String HUNTER_TEAM_NAME = "infmanhunt_hunters";
    private static final String RUNNER_TEAM_NAME = "infmanhunt_runners";
    private static final String TRACKER_TAG = "tracker";
    private static final ItemStack TRACKER_ITEM = new ItemUtils.Builder(Material.COMPASS)
            .withDisplayName(Notifications.getTrackerDisplayName())
            .addMetaTransform(itemMeta -> {
                if (itemMeta instanceof CompassMeta meta) meta.setLodestoneTracked(false);
            })
            .withTag(TRACKER_TAG)
            .appendLore("Left click - Update tracker")
            .appendLore("Right click - Track next runner")
            .build();

    private final SettingsRegistry gameSettings;
    private final PluginResourceManager resourceManager;
    private final IPlayerTrackingStrategy trackingStrategy;
    private final Map<OptionalOnlinePlayer, Integer> runnerTrackingIndexes;

    private final Set<OptionalOnlinePlayer> hunters;
    private final List<OptionalOnlinePlayer> runners;

    private final Scoreboard gameScoreboard;

    private int runnerLivesLeft;
    private boolean isRunning;

    private Game(
            PluginResourceManager resourceManager,
            Set<OptionalOnlinePlayer> hunters,
            Set<OptionalOnlinePlayer> runners,
            IPlayerTrackingStrategy trackingStrategy
    ) {
        this.gameSettings = SettingsRegistry.getDefaultRegistry();
        this.resourceManager = resourceManager;
        this.hunters = new HashSet<>(hunters);
        this.runners = new ArrayList<>(runners);
        this.trackingStrategy = trackingStrategy;
        this.runnerTrackingIndexes = new HashMap<>();
        this.resourceManager.registerEventListeners(this, trackingStrategy);

        this.gameScoreboard = Optional.ofNullable(Bukkit.getScoreboardManager()).orElseThrow(
                () -> new InfectedManhuntException("ScoreboardManager is null")
        ).getNewScoreboard();

        setupGame();
    }

    private void setupGame() {
        // TODO: set settings elsewhere
        gameSettings.set(GameSetting.INFECT_RUNNERS, true);

        Team hunterTeam = gameScoreboard.registerNewTeam(HUNTER_TEAM_NAME);
        hunterTeam.setDisplayName(Notifications.getHunterTeamName());
        hunterTeam.setColor(Notifications.getHunterTeamColor());
        hunterTeam.setPrefix(Notifications.getHunterTeamPrefix(gameSettings.get(GameSetting.INFECT_RUNNERS)));

        Team runnerTeam = gameScoreboard.registerNewTeam(RUNNER_TEAM_NAME);
        runnerTeam.setDisplayName(Notifications.getRunnerTeamName());
        runnerTeam.setColor(Notifications.getRunnerTeamColor());
        runnerTeam.setPrefix(Notifications.getRunnerTeamPrefix());

        for (OptionalOnlinePlayer participant : getParticipants()) {
            if (isHunter(participant)) {
                hunterTeam.addEntry(participant.getName());
                advanceTrackedRunner(participant);
            } else if (isRunner(participant)) {
                runnerTeam.addEntry(participant.getName());
            }
            participant.run(p -> p.setScoreboard(gameScoreboard));
        }

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.LOCATOR_BAR, false);
        }

        runnerLivesLeft = gameSettings.get(GameSetting.RUNNER_LIVES);
        isRunning = true;

        WinCondition<?> winCondition = gameSettings.get(GameSetting.RUNNER_WIN_CONDITION);
        resourceManager.registerEventListener(winCondition);
        winCondition.setCallback(() -> endGame(true));
    }

    private Iterable<OptionalOnlinePlayer> getParticipants() {
        return new Iterable<>() {
            @Override
            public @NonNull Iterator<OptionalOnlinePlayer> iterator() {
                return Iterators.concat(runners.iterator(), hunters.iterator());
            }
        };
    }

    private boolean isHunter(OptionalOnlinePlayer player) {
        return hunters.contains(player);
    }

    private boolean isRunner(OptionalOnlinePlayer player) {
        return runners.contains(player);
    }

    private boolean isParticipant(OptionalOnlinePlayer player) {
        return isHunter(player) || isRunner(player);
    }

    public void giveTrackerTo(OptionalOnlinePlayer p) {
        p.run(player -> player.getInventory().addItem(TRACKER_ITEM));
    }

    private void onPlayerDeath(OptionalOnlinePlayer p, Player killer) {
        if (!isRunner(p)) return;
        runnerLivesLeft--;
        Iterable<OptionalOnlinePlayer> participants = getParticipants();
        if (gameSettings.get(GameSetting.INFECT_RUNNERS)) {
            infectRunner(p);
            Notifications.announceInfection(participants, p, killer);
            if (runners.isEmpty()) {
                endGame(false);
            } else {
                Sounds.PLAYER_ELIMINATION.playTo(participants);
            }
        } else {
            if (runnerLivesLeft <= 0) {
                endGame(false);
            } else {
                Notifications.announceRunnerDeath(participants, p, killer, runnerLivesLeft);
                Sounds.PLAYER_ELIMINATION.playTo(participants);
            }
        }
    }

    private void infectRunner(OptionalOnlinePlayer runner) {
        runners.remove(runner);
        hunters.add(runner);
        Optional.ofNullable(gameScoreboard.getTeam(RUNNER_TEAM_NAME))
                .ifPresent(team -> team.removeEntry(runner.getName()));
        Optional.ofNullable(gameScoreboard.getTeam(HUNTER_TEAM_NAME))
                .ifPresent(team -> team.addEntry(runner.getName()));
    }

    private void endGame(boolean runnersWin) {
        if (!isRunning) return;
        isRunning = false;

        Iterable<OptionalOnlinePlayer> participants = getParticipants();
        if (runnersWin) {
            Notifications.announceRunnersWin(participants);
            Sounds.GAME_END.playTo(participants);
        } else {
            Notifications.announceHuntersWin(participants);
            Sounds.WITHER_SPAWN.playTo(participants);
        }
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player playerVictim)) return;
        OptionalOnlinePlayer victim = OptionalOnlinePlayer.of(playerVictim);
        if (!isParticipant(victim)) return;
        if (playerVictim.getHealth() - event.getFinalDamage() <= 0) {
            if (event.getDamager() instanceof Player killer)
                onPlayerDeath(victim, killer);
            else
                onPlayerDeath(victim, null);
        }
    }

    @EventHandler
    public void onTrackerClick(PlayerInteractEvent event) {
        OptionalOnlinePlayer p = OptionalOnlinePlayer.of(event.getPlayer());
        ItemStack item = event.getItem();

        if (!ItemUtils.containsTag(item, TRACKER_TAG)) return;
        if (!isHunter(p)) {
            Notifications.sendTrackingError(p, "Only hunters can use trackers!");
            return;
        };
        if (event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            // tracker left click -> cycle to next runner
            advanceTrackedRunner(p);
        }
        if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
            || event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            // tracker left/right click -> update tracker
            TrackingResult result = trackingStrategy.query(new TrackingRequest(
                    p,
                    getTrackedRunner(p),
                    (CompassMeta) item.getItemMeta(),
                    item::setItemMeta
            ));
            if (result.success()) {
                Notifications.sendTrackingNotification(p, result.message());
            } else {
                Notifications.sendTrackingError(p, result.message());
            }
        }
    }

    private OptionalOnlinePlayer advanceTrackedRunner(OptionalOnlinePlayer hunter) throws InfectedManhuntException {
        if (runners.isEmpty()) throw new InfectedManhuntException("No runners left!");
        int currentIndex = runnerTrackingIndexes.getOrDefault(hunter, -1);
        currentIndex = (currentIndex + 1) % runners.size();
        runnerTrackingIndexes.put(hunter, currentIndex);
        return runners.get(currentIndex);
    }

    private OptionalOnlinePlayer getTrackedRunner(OptionalOnlinePlayer hunter) {
        int index = runnerTrackingIndexes.getOrDefault(hunter, 0);
        return runners.get(index);
    }

    public void destroy() {
        Scoreboard mainScoreboard = Optional.ofNullable(Bukkit.getScoreboardManager())
                .map(ScoreboardManager::getMainScoreboard).orElse(null);
        if (mainScoreboard != null) {
            for (OptionalOnlinePlayer participant : getParticipants()) {
                participant.run(p -> p.setScoreboard(mainScoreboard));
            }
        }
    }

    public static class Builder {
        private final PluginResourceManager resourceManager;
        private final Set<OptionalOnlinePlayer> hunters;
        private final Set<OptionalOnlinePlayer> runners;
        private IPlayerTrackingStrategy trackingStrategy;

        public Builder(PluginResourceManager resourceManager) {
            this.resourceManager = resourceManager;
            this.hunters = new HashSet<>();
            this.runners = new HashSet<>();
            this.trackingStrategy = null;
        }

        public Builder setHunters(OptionalOnlinePlayer... players) {
            for (OptionalOnlinePlayer player : players) {
                runners.remove(player);
                hunters.add(player);
            }
            return this;
        }

        public Builder setRunners(OptionalOnlinePlayer... players) {
            for (OptionalOnlinePlayer player : players) {
                hunters.remove(player);
                runners.add(player);
            }
            return this;
        }

        public Builder setTrackingStrategy(IPlayerTrackingStrategy strategy) {
            trackingStrategy = strategy;
            return this;
        }

        public Game build() {
            return new Game(
                    resourceManager,
                    hunters,
                    runners,
                    trackingStrategy
            );
        }
    }
}
