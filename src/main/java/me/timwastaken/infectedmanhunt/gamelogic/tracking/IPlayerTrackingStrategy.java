package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import org.bukkit.event.Listener;

public interface IPlayerTrackingStrategy extends Listener {
    TrackingResult query(TrackingRequest request);
}
