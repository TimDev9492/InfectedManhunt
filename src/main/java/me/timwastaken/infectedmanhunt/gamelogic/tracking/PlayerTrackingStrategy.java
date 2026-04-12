package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import me.timwastaken.infectedmanhunt.serialization.RegistryEnum;
import org.bukkit.event.Listener;

public abstract class PlayerTrackingStrategy implements Listener {
    public abstract TrackingResult query(TrackingRequest request);

    @FunctionalInterface
    interface Provider {
        PlayerTrackingStrategy provide(PluginResourceManager manager);
    }

    public enum Registry implements RegistryEnum {
        LAZY(_manager -> new LazyPlayerTrackingStrategy()),
        PORTAL(_manager -> new PortalEntranceTrackingStrategy()),
        AUTO_UPDATE(manager -> new ContinuousTrackingStrategy(
                manager, new PortalEntranceTrackingStrategy()
        ));

        private final PlayerTrackingStrategy.Provider provider;

        Registry(PlayerTrackingStrategy.Provider provider) {
            this.provider = provider;
        }

        public PlayerTrackingStrategy getImplementation(PluginResourceManager resourceManager) {
            return provider.provide(resourceManager);
        }

        public String identifier() {
            return name().toLowerCase();
        }

        public static PlayerTrackingStrategy.Registry fromIdentifier(String identifier) {
            try {
                return PlayerTrackingStrategy.Registry.valueOf(identifier.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        @Override
        public String toString() {
            return identifier();
        }
    }
}
