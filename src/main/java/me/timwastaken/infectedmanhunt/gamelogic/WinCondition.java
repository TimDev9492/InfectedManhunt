package me.timwastaken.infectedmanhunt.gamelogic;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;

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
}
