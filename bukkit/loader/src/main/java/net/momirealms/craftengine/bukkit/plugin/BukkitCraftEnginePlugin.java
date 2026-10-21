package net.momirealms.craftengine.bukkit.plugin;

import net.momirealms.craftengine.core.util.ByteBuddyUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitCraftEnginePlugin extends JavaPlugin {
    static {
        ByteBuddyUtils.configureUnsafeInjection();
    }

    private final BukkitCraftEngine plugin;

    public BukkitCraftEnginePlugin() {
        this.plugin = new BukkitCraftEngine(this);
        this.plugin.applyDependencies();
        this.plugin.setUpConfigAndLocale();
        this.plugin.setupProxy();
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
