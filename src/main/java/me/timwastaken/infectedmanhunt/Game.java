package me.timwastaken.infectedmanhunt;

import com.google.common.collect.Iterators;
import me.timwastaken.infectedmanhunt.common.ItemUtils;
import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.common.Utils;
import me.timwastaken.infectedmanhunt.exceptions.InfectedManhuntException;
import me.timwastaken.infectedmanhunt.gamelogic.wincondition.WinCondition;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Predicate;

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
    private boolean started = false;
    private boolean isRunning = false;
    private final Set<OptionalOnlinePlayer> receivedFirstFallDamage;
    private boolean lockHunterMovement = false;
    private Predicate<Location> tooFarFromSpawn = null;

    private Game(
            World startWorld,
            PluginResourceManager resourceManager,
            Set<OptionalOnlinePlayer> hunters,
            Set<OptionalOnlinePlayer> runners,
            IPlayerTrackingStrategy trackingStrategy,
            SettingsRegistry gameSettings
    ) {
        this.gameSettings = gameSettings;
        this.resourceManager = resourceManager;
        this.hunters = new HashSet<>(hunters);
        this.runners = new ArrayList<>(runners);
        this.trackingStrategy = trackingStrategy;
        this.runnerTrackingIndexes = new HashMap<>();
        this.receivedFirstFallDamage = new HashSet<>();

        this.gameScoreboard = Optional.ofNullable(Bukkit.getScoreboardManager()).orElseThrow(
                () -> new InfectedManhuntException("ScoreboardManager is null")
        ).getNewScoreboard();

        setupGame(startWorld);
    }

    private void setupGame(World startWorld) {
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

        startWorld.setTime(0L);
        for (OptionalOnlinePlayer participant : getParticipants()) {
            participant.run(p -> {
                p.setScoreboard(gameScoreboard);
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.setHealth(20);
                p.setFoodLevel(20);
                p.setSaturation(5);
                p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
            });
            if (isHunter(participant)) {
                hunterTeam.addEntry(participant.getName());
                advanceTrackedRunner(participant);
                giveTrackerTo(participant);
            } else if (isRunner(participant)) {
                runnerTeam.addEntry(participant.getName());
            }
        }
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.LOCATOR_BAR, false);
        }

        runnerLivesLeft = gameSettings.get(GameSetting.RUNNER_LIVES);

        double startY = 256;
        double radius = 4;
        Location startLoc = startWorld.getSpawnLocation().clone();
        startLoc.setY(startY);
        Location onCircle = Utils.constructCircle(
                startLoc,
                Material.SMOOTH_STONE,
                Material.GRAY_STAINED_GLASS,
                radius + 0.5
        );
        if (onCircle == null) throw new InfectedManhuntException("Failed to build start platform.");
        for (OptionalOnlinePlayer runner : runners) {
            runner.run(p -> p.teleport(onCircle));
        }
        Utils.spreadPlayersInCircle(
                new ArrayList<>(hunters),
                onCircle,
                radius,
                onCircle.getY()
        );
        lockHunterMovement = true;
        double startRadius = radius + 2;
        tooFarFromSpawn = loc -> loc.distanceSquared(onCircle) > startRadius * startRadius;

        started = true;
        resourceManager.registerEventListeners(this, trackingStrategy);
        WinCondition<?> winCondition = gameSettings.get(GameSetting.RUNNER_WIN_CONDITION).getImplementation();
        resourceManager.registerEventListener(winCondition);
        winCondition.setCallback(() -> endGame(true));
    }

    private void startGame() {
        isRunning = true;
        Iterable<OptionalOnlinePlayer> participants = getParticipants();
        Notifications.announceGameStart(participants);
        int headstart = gameSettings.get(GameSetting.RUNNER_HEADSTART_SECONDS);
        Sounds.RESUME_GAME.playTo(participants);
        if (headstart > 0) {
            Notifications.announceRunnerHeadstart(participants, headstart);
            resourceManager.runTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    lockHunterMovement = false;
                    Notifications.announceHunterRelease(participants);
                    Sounds.FIGHT_ANNOUNCEMENT.playTo(participants);
                }
            }, headstart * 20L);
        } else {
            lockHunterMovement = false;
        }

        resourceManager.runTaskTimer(new BukkitRunnable() {
            private long elapsedSeconds = 0;

            @Override
            public void run() {
                if (isRunning) {
                    updateTabList(elapsedSeconds++);
                } else {
                    this.cancel();
                }
            }
        }, 0L, 20L);
    }

    private void updateTabList(long elapsedTime) {
        for (OptionalOnlinePlayer participant : getParticipants()) {
            if (!participant.isOnline()) continue;
            Player p = participant.get();
            p.setPlayerListHeaderFooter(
                    Notifications.getListHeader(),
                    Notifications.getListFooter(elapsedTime)
            );
        }
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

    private void processPlayerDeath(OptionalOnlinePlayer p, Player killer) {
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
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        OptionalOnlinePlayer p = OptionalOnlinePlayer.of(event.getPlayer());
        if (!started) return;
        if (!isRunning && isRunner(p) && tooFarFromSpawn != null) {
            if (tooFarFromSpawn.test(event.getTo())) startGame();
        }
        if (lockHunterMovement && isHunter(p)) {
            if (event.getFrom().distanceSquared(event.getTo()) > 0) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player playerVictim)) return;
        OptionalOnlinePlayer victim = OptionalOnlinePlayer.of(playerVictim);
        if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL) && !receivedFirstFallDamage.contains(victim)) {
            event.setCancelled(true);
            receivedFirstFallDamage.add(victim);
            return;
        }
        if (!isParticipant(victim)) return;
        if (playerVictim.getHealth() - event.getFinalDamage() <= 0) {
            if (event instanceof EntityDamageByEntityEvent byEntityEvent
                    && byEntityEvent.getDamager() instanceof Player killer)
                processPlayerDeath(victim, killer);
            else
                processPlayerDeath(victim, null);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        OptionalOnlinePlayer p = OptionalOnlinePlayer.of(event.getEntity());
        if (!isParticipant(p)) return;
        Predicate<ItemStack> isTracker = item -> ItemUtils.containsTag(item, TRACKER_TAG);
        ItemUtils.removeIf(event.getEntity().getInventory(), isTracker);
        event.getDrops().removeIf(isTracker);
        if (isHunter(p) && gameSettings.get(GameSetting.HUNTER_KEEP_INVENTORY)
            || isRunner(p) && gameSettings.get(GameSetting.RUNNER_KEEP_INVENTORY)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        OptionalOnlinePlayer p = OptionalOnlinePlayer.of(event.getPlayer());
        if (isHunter(p)) giveTrackerTo(p);
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

    public boolean isStarted() {
        return started;
    }

    public static class Builder {
        private final PluginResourceManager resourceManager;
        private final Set<OptionalOnlinePlayer> hunters;
        private final Set<OptionalOnlinePlayer> runners;
        private World world;
        private IPlayerTrackingStrategy trackingStrategy;
        private SettingsRegistry settingsRegistry;

        public Builder(PluginResourceManager resourceManager) {
            this.resourceManager = resourceManager;
            this.hunters = new HashSet<>();
            this.runners = new HashSet<>();
            this.trackingStrategy = null;
        }

        public Builder setWorld(World world) {
            this.world = world;
            return this;
        }

        public Builder setSettings(SettingsRegistry registry) {
            this.settingsRegistry = registry;
            return this;
        }

        public SettingsRegistry getSettings() {
            return settingsRegistry;
        }

        public <T> Builder changeSetting(GameSetting<T> setting, T value) {
            settingsRegistry.set(setting, value);
            return this;
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
                    world,
                    resourceManager,
                    hunters,
                    runners,
                    trackingStrategy,
                    settingsRegistry
            );
        }
    }
}
