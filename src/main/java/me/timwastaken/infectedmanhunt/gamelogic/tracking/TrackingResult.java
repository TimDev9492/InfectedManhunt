package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import org.bukkit.Location;
import org.bukkit.inventory.meta.CompassMeta;

public record TrackingResult(boolean success, String message, CompassMeta updatedMeta) {
}
