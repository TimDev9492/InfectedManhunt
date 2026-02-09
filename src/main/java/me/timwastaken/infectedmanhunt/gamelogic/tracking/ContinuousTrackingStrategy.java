package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repeat the last query every tick.
 */
public class ContinuousTrackingStrategy implements IPlayerTrackingStrategy {
    private final IPlayerTrackingStrategy childStrategy;
    private final Map<OptionalOnlinePlayer, TrackingRequest> latestRequests;

    public ContinuousTrackingStrategy(PluginResourceManager resourceManager, IPlayerTrackingStrategy childStrategy) {
        this.childStrategy = childStrategy;
        this.latestRequests = new ConcurrentHashMap<>();
        resourceManager.registerEventListener(childStrategy);
        resourceManager.runTaskTimer(new BukkitRunnable() {
            @Override
            public void run() {
                replayRequests();
            }
        }, 0L, 1L);
    }

    @Override
    public TrackingResult query(TrackingRequest request) {
        latestRequests.put(request.tracking(), request);
        return childStrategy.query(request);
    }

    private void replayRequests() {
        for (TrackingRequest latestPlayerRequest : latestRequests.values()) {
            childStrategy.query(latestPlayerRequest);
        }
    }
}
