package net.momirealms.craftengine.bukkit;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitCraftEnginePlugin extends JavaPlugin {
    private final BukkitCraftEngine plugin;

    public BukkitCraftEnginePlugin() {
        this.plugin = new BukkitCraftEngine(this);
        this.plugin.setJavaPlugin(this);
    }

    @Override
    public void onLoad() {
        this.plugin.onPluginLoad();
    }

    @Override
    public void onEnable() {
        this.plugin.onPluginEnable();
    }

    @Override
    public void onDisable() {
        this.plugin.onPluginDisable();
    }
}
