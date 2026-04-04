package net.litecraft;

import org.bukkit.plugin.java.JavaPlugin;
import net.litecraft.registry.LiteRegistry;
import net.litecraft.examples.ExampleContent;

public class LiteCraft extends JavaPlugin {

    private LiteRegistry registry;

    @Override
    public void onEnable() {
        this.registry = new LiteRegistry(this);

        // Initialize systems
        ExampleContent.registerAll(this.registry);

        getLogger().info("LiteCraft enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("LiteCraft disabled.");
    }

    public LiteRegistry getRegistry() {
        return registry;
    }
}
