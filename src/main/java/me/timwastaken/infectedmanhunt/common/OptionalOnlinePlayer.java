package me.timwastaken.infectedmanhunt.common;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class OptionalOnlinePlayer {
    private final UUID playerUUID;

    public static OptionalOnlinePlayer of(Player p) {
        return new OptionalOnlinePlayer(p.getUniqueId());
    }

    public static OptionalOnlinePlayer of(UUID uuid) {
        return new OptionalOnlinePlayer(uuid);
    }

    private OptionalOnlinePlayer(UUID uuid) {
        this.playerUUID = uuid;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public OfflinePlayer getOffline() {
        return Bukkit.getOfflinePlayer(this.playerUUID);
    }

    public String getName() {
        return Optional.ofNullable(getOffline().getName()).orElse("Unknown");
    }

    public Player get() {
        return Bukkit.getPlayer(this.playerUUID);
    }

    public boolean isOnline() {
        return this.get() != null;
    }

    public <T> Optional<T> map(Function<Player, T> transform) {
        Player p = this.get();
        if (p == null) return Optional.empty();
        return Optional.of(transform.apply(p));
    }

    public boolean run(Consumer<Player> function) {
        Player p = this.get();
        if (p == null) return false;
        function.accept(p);
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Player other) {
            return this.playerUUID.equals(other.getUniqueId());
        } else if (obj instanceof OptionalOnlinePlayer optionalOnlinePlayer) {
            return optionalOnlinePlayer.getPlayerUUID().equals(this.getPlayerUUID());
        }
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        return this.playerUUID.hashCode();
    }
}
