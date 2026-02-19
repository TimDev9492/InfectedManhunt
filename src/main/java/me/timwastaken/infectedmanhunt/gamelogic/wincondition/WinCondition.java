package me.timwastaken.infectedmanhunt.gamelogic.wincondition;

import me.timwastaken.infectedmanhunt.Game;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
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
        WinCondition<? extends Event> provide(Predicate<Player> runnerCheck);
    }

    public enum Registry {
        KILL_ENDER_DRAGON(_check -> new KillEnderDragonCondition()),
        ADVANCEMENT_UNEASY_ALLIANCE(check -> new AdvancementCondition(
                NamespacedKey.minecraft("nether/uneasy_alliance"),
                check
        )),
        ADVANCEMENT_UNLOCK_OMINOUS_VAULT(check -> new AdvancementCondition(
                NamespacedKey.minecraft("adventure/revaulting"),
                check
        )),
        ADVANCEMENT_CURE_ZOMBIE_VILLAGER(check -> new AdvancementCondition(
                NamespacedKey.minecraft("story/cure_zombie_villager"),
                check
        )),
        ADVANCEMENT_HERO_OF_THE_VILLAGE(check -> new AdvancementCondition(
                NamespacedKey.minecraft("adventure/hero_of_the_village"),
                check
        )),
        ADVANCEMENT_PLAY_MUSIC_DISC_IN_MEADOWS(check -> new AdvancementCondition(
                NamespacedKey.minecraft("adventure/play_jukebox_in_meadows"),
                check
        ));

        private final WinConditionProvider provider;

        Registry(WinConditionProvider provider) {
            this.provider = provider;
        }

        public WinCondition<?> getImplementation(Predicate<Player> runnerCheck) {
            return provider.provide(runnerCheck);
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
