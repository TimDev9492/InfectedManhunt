package me.timwastaken.infectedmanhunt;

import com.google.common.collect.Iterators;
import me.timwastaken.infectedmanhunt.common.ItemUtils;
import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.exceptions.InfectedManhuntException;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.IPlayerTrackingStrategy;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.TrackingRequest;
import me.timwastaken.infectedmanhunt.gamelogic.tracking.TrackingResult;
import me.timwastaken.infectedmanhunt.ui.Notifications;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class Game implements Listener {
    private static final String INFECTED_TEAM_NAME = "infectedmanhunt_inf";
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

    private final PluginResourceManager resourceManager;
    private final IPlayerTrackingStrategy trackingStrategy;
    private final Map<OptionalOnlinePlayer, Integer> runnerTrackingIndexes;

    private final Set<OptionalOnlinePlayer> infected;
    private final List<OptionalOnlinePlayer> runners;

    private final Scoreboard gameScoreboard;

    private Game(
            PluginResourceManager resourceManager,
            Set<OptionalOnlinePlayer> infected,
            Set<OptionalOnlinePlayer> runners,
            IPlayerTrackingStrategy trackingStrategy
    ) {
        this.resourceManager = resourceManager;
        this.infected = Set.copyOf(infected);
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
        Team infectedTeam = gameScoreboard.registerNewTeam(INFECTED_TEAM_NAME);
        infectedTeam.setDisplayName(Notifications.getInfectedTeamName());
        infectedTeam.setColor(Notifications.getInfectedTeamColor());
        infectedTeam.setPrefix(Notifications.getInfectedTeamPrefix());
        for (OptionalOnlinePlayer hunter : infected) {
            infectedTeam.addEntry(hunter.getName());
            advanceTrackedRunner(hunter);
        }
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.LOCATOR_BAR, false);
        }
        for (OptionalOnlinePlayer participant : getParticipants()) {
            participant.run(p -> p.setScoreboard(gameScoreboard));
        }
    }

    private Iterable<OptionalOnlinePlayer> getParticipants() {
        return new Iterable<>() {
            @Override
            public @NonNull Iterator<OptionalOnlinePlayer> iterator() {
                return Iterators.concat(runners.iterator(), infected.iterator());
            }
        };
    }

    private boolean isHunter(OptionalOnlinePlayer player) {
        return infected.contains(player);
    }

    private boolean isRunner(OptionalOnlinePlayer player) {
        return runners.contains(player);
    }

    public void giveTrackerTo(OptionalOnlinePlayer p) {
        p.run(player -> player.getInventory().addItem(TRACKER_ITEM));
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
        private final Set<OptionalOnlinePlayer> infected;
        private final Set<OptionalOnlinePlayer> runners;
        private IPlayerTrackingStrategy trackingStrategy;

        public Builder(PluginResourceManager resourceManager) {
            this.resourceManager = resourceManager;
            this.infected = new HashSet<>();
            this.runners = new HashSet<>();
            this.trackingStrategy = null;
        }

        public Builder setInfected(OptionalOnlinePlayer... players) {
            for (OptionalOnlinePlayer player : players) {
                runners.remove(player);
                infected.add(player);
            }
            return this;
        }

        public Builder setRunners(OptionalOnlinePlayer... players) {
            for (OptionalOnlinePlayer player : players) {
                infected.remove(player);
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
                    infected,
                    runners,
                    trackingStrategy
            );
        }
    }
}
