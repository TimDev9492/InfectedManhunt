package me.author.minecraftPluginTemplate;

import me.author.minecraftPluginTemplate.common.PluginResourceManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftPluginTemplate extends JavaPlugin {
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
