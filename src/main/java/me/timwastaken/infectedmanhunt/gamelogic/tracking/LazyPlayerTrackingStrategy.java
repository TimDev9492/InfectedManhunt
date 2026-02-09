package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.Objects;

public class LazyPlayerTrackingStrategy implements IPlayerTrackingStrategy {
    @Override
    public TrackingResult query(TrackingRequest request) {
        if (!request.tracking().isOnline()) throw new IllegalStateException(
                "Cannot send tracking request for an offline player!"
        );
        CompassMeta meta = request.trackerMeta();
        meta.setDisplayName(String.format(
                "%sTracking %s%s",
                ChatColor.GRAY,
                ChatColor.GREEN,
                request.target().getName()
        ));
        if (!request.target().isOnline()) {
            return new TrackingResult(
                    false,
                    String.format(
                            "%s%s %sis offline",
                            ChatColor.DARK_RED,
                            request.target().getName(),
                            ChatColor.RED
                    ),
                    null
            );
        }
        Location trackingLocation = request.tracking().get().getLocation();
        Location targetLocation = trackPlayer(trackingLocation, request.target().get());
        meta.setLodestone(targetLocation);
        request.metaUpdater().accept(meta);
        if (!Objects.equals(trackingLocation.getWorld(), request.target().get().getLocation().getWorld())) {
            return new TrackingResult(
                    false,
                    String.format(
                            "%s%s %sis in another dimension",
                            ChatColor.DARK_RED,
                            request.target().getName(),
                            ChatColor.RED
                    ),
                    meta
            );
        }
        return new TrackingResult(
                true,
                String.format(
                        "%sPointing towards %s%s",
                        ChatColor.GRAY,
                        ChatColor.GREEN,
                        request.target().getName()
                ),
                meta
        );
    }

    protected Location trackPlayer(Location from, Player target) {
        Location targetPlayerLocation = target.getLocation();
        if (Objects.equals(from.getWorld(), targetPlayerLocation.getWorld())) return null;
        return targetPlayerLocation.clone();
    }
}
