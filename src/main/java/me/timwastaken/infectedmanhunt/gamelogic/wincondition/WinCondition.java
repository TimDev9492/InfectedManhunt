package me.timwastaken.infectedmanhunt.gamelogic.wincondition;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;
import java.util.function.Predicate;

public abstract class WinCondition<T extends Event> implements Listener {
    protected final Predicate<T> contextCheck;
    protected Runnable callback = null;

    public WinCondition(Predicate<T> contextCheck) {
        this.contextCheck = contextCheck;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    @FunctionalInterface
    interface WinConditionProvider {
        WinCondition<? extends Event> provide();
    }

    public enum Registry {
        KILL_ENDER_DRAGON(KillEnderDragonCondition::new);

        private final WinCondition<?> impl;

        Registry(WinConditionProvider provider) {
            this.impl = provider.provide();
        }

        public WinCondition<?> getImplementation() {
            return impl;
        }

        public String identifier() {
            return name().toLowerCase();
        }

        public static Registry fromIdentifier(String identifier) {
            try {
                return Registry.valueOf(identifier.toUpperCase());
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
