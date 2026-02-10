package me.timwastaken.infectedmanhunt.common;

import me.timwastaken.infectedmanhunt.Game;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PluginResourceManager {
    private final JavaPlugin plugin;
    private final List<Integer> taskIds;
    private Game activeGame;

    public PluginResourceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.taskIds = new ArrayList<>();
    }

    public void registerEventListener(Listener listener) {
        this.plugin.getServer().getPluginManager().registerEvents(listener, this.plugin);
    }

    public void registerEventListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            this.registerEventListener(listener);
        }
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(this.plugin);
    }

    public void registerTask(int taskId) {
        this.taskIds.add(taskId);
    }

    public void runTaskLater(BukkitRunnable runnable, long delay) {
        this.registerTask(runnable.runTaskLater(this.plugin, delay).getTaskId());
    }

    public void runTaskTimerAsynchronously(BukkitRunnable runnable, long delay, long period) {
        this.registerTask(runnable.runTaskTimerAsynchronously(this.plugin, delay, period).getTaskId());
    }

    public void runTaskTimer(BukkitRunnable runnable, long delay, long period) {
        this.registerTask(runnable.runTaskTimer(this.plugin, delay, period).getTaskId());
    }

    public void unregisterTasks() {
        for (int taskId : this.taskIds) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void destroyGame() {
        if (activeGame != null) activeGame.destroy();
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
        Optional.ofNullable(this.plugin.getCommand(commandName))
                .ifPresent(command -> command.setExecutor(executor));
    }

    public boolean hasGameStarted() {
        return activeGame != null && activeGame.isStarted();
    }

    public void setActiveGame(Game game) {
        activeGame = game;
    }

    public Game getActiveGame() {
        return activeGame;
    }
}
