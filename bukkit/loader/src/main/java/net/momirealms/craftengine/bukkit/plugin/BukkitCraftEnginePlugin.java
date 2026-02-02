package net.momirealms.craftengine.bukkit.plugin;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitCraftEnginePlugin extends JavaPlugin {
    private final BukkitCraftEngine plugin;

    private BukkitCraftEnginePlugin() {
        ReflectionHelper.init();
        this.plugin = new BukkitCraftEngine(this);
        this.plugin.applyDependencies();
        this.plugin.setUpConfigAndLocale();
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
