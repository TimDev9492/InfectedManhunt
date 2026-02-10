package me.timwastaken.infectedmanhunt.common;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class Utils {
    public static Location constructCircle(Location center, Material dot, Material fill, double radius) {
        World world = center.getWorld();
        if (world == null) return null;
        Location centered = new Location(
                world,
                center.getBlockX() + 0.5,
                center.getBlockY(),
                center.getBlockZ() + 0.5
        );
        int range = (int) Math.ceil(radius);
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                Location current = centered.clone().add(x, 0, z);
                if (x == 0 && z == 0) {
                    world.getBlockAt(current).setType(dot);
                    System.out.println("Placing dot at " + current.toString());
                } else if (current.distanceSquared(centered) <= radius * radius) {
                    world.getBlockAt(current).setType(fill);
                }
            }
        }
        centered.add(0, 1, 0);
        return centered;
    }

    public static void spreadPlayersInCircle(
            List<OptionalOnlinePlayer> participants,
            Location center,
            double radius,
            Double concreteY
    ) {
        World world = center.getWorld();
        // specific edge case: ensure list isn't empty to avoid division by zero
        if (participants == null || world == null) {
            return;
        }

        List<Player> players = participants.stream()
                .filter(OptionalOnlinePlayer::isOnline)
                .map(OptionalOnlinePlayer::get)
                .toList();

        // center middle of block
        center = center.clone();
        center.setX(center.getBlockX() + 0.5);
        center.setZ(center.getBlockZ() + 0.5);

        // Calculate the angle increment (in radians) for each player
        // 360 degrees = 2 * PI radians
        double angleIncrement = (2 * Math.PI) / players.size();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            // Calculate current angle for this specific player
            double angle = i * angleIncrement;

            // Calculate new X and Z coordinates
            // cos(angle) gives x component, sin(angle) gives z component
            double exactX = center.getX() + (radius * Math.cos(angle));
            double exactZ = center.getZ() + (radius * Math.sin(angle));
            int x = Math.toIntExact(Math.round(exactX));
            int z = Math.toIntExact(Math.round(exactZ));
            double y = concreteY == null ? world.getHighestBlockYAt(x, z) + 1 : concreteY;

            // Create the target location object
            Location targetLocation = new Location(world, exactX, y, exactZ);

            // MATHEMATICALLY FACE INWARDS:
            // To face a target, we calculate the vector: TargetPosition - CurrentPosition
            Vector direction = center.toVector().subtract(targetLocation.toVector());
            targetLocation.setDirection(direction);

            // Teleport the player
            player.teleport(targetLocation);
        }
    }
}
