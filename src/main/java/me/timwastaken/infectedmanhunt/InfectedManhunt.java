package me.timwastaken.infectedmanhunt;

import me.timwastaken.infectedmanhunt.common.PluginResourceManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfectedManhunt extends JavaPlugin {
    private PluginResourceManager resourceManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        resourceManager = new PluginResourceManager(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        resourceManager.unregisterTasks();
        resourceManager.unregisterEvents();
    }
}
