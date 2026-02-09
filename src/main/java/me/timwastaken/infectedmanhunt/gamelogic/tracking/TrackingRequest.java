package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

public record TrackingRequest(
        OptionalOnlinePlayer tracking,
        OptionalOnlinePlayer target,
        CompassMeta trackerMeta,
        Consumer<CompassMeta> metaUpdater
) {
}
