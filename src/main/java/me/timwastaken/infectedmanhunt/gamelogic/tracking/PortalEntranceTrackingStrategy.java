package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import me.timwastaken.infectedmanhunt.common.OptionalOnlinePlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PortalEntranceTrackingStrategy extends LazyPlayerTrackingStrategy {
    private final Map<World.Environment, Map<OptionalOnlinePlayer, Location>> portalExitsByWorldType;

    public PortalEntranceTrackingStrategy() {
        this.portalExitsByWorldType = new HashMap<>();
    }

    @Override
    protected Location trackPlayer(Location from, Player target) {
        Location targetPlayerLocation = target.getLocation();
        if (Objects.equals(from.getWorld(), targetPlayerLocation.getWorld())) return targetPlayerLocation;
        Optional<World.Environment> fromEnvOpt = Optional.ofNullable(from.getWorld()).map(World::getEnvironment);
        if (fromEnvOpt.isEmpty()) return null;
        World.Environment fromEnv = fromEnvOpt.get();
        return getPortalExits(fromEnv).get(OptionalOnlinePlayer.of(target));
    }

    private Map<OptionalOnlinePlayer, Location> getPortalExits(World.Environment environment) {
        if (!portalExitsByWorldType.containsKey(environment)) portalExitsByWorldType.put(environment, new HashMap<>());
        return portalExitsByWorldType.get(environment);
    }

    @EventHandler
    public void onPlayerEnterPortal(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getWorld() == null) return;
        if (from.getWorld().equals(to.getWorld())) return;
        Map<OptionalOnlinePlayer, Location> playerPortalExits = getPortalExits(from.getWorld().getEnvironment());
        playerPortalExits.put(OptionalOnlinePlayer.of(event.getPlayer()), from.clone());
    }
}
