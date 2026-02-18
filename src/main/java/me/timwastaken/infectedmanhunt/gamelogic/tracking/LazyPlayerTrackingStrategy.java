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
                "%s%sTracking %s%s%s",
                ChatColor.GRAY,
                ChatColor.BOLD,
                ChatColor.LIGHT_PURPLE,
                ChatColor.BOLD,
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
                    )
            );
        }
        Location trackingLocation = request.tracking().get().getLocation();
        Location targetLocation = trackPlayer(trackingLocation, request.target().get());
        if (targetLocation == null) {
            return new TrackingResult(
                    false,
                    String.format(
                            "%s%s %sis in another dimension",
                            ChatColor.DARK_RED,
                            request.target().getName(),
                            ChatColor.RED
                    )
            );
        }
        meta.setLodestone(targetLocation);
        request.metaUpdater().accept(meta);
        return new TrackingResult(
                true,
                String.format(
                        "%s%sTracker updated",
                        ChatColor.GREEN,
                        ChatColor.ITALIC
                )
        );
    }

    protected Location trackPlayer(Location from, Player target) {
        Location targetPlayerLocation = target.getLocation();
        if (!Objects.equals(from.getWorld(), targetPlayerLocation.getWorld())) return null;
        return targetPlayerLocation.clone();
    }
}
