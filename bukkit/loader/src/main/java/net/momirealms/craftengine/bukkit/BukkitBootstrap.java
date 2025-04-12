package net.momirealms.craftengine.bukkit;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class BukkitBootstrap extends JavaPlugin {
    private final BukkitCraftEngine plugin;
    private Object instance$dedicatedServer;
    private final Field field$MinecraftServer$onlineMode;

    public BukkitBootstrap() {
        this.plugin = new BukkitCraftEngine(this);
        this.field$MinecraftServer$onlineMode = requireNonNull(
                ReflectionUtils.getDeclaredField(Reflections.clazz$MinecraftServer, boolean.class, 5)
        );
    }

    @Override
    public void onLoad() {
        try {
            if (Bukkit.getServer().getMaxPlayers() > 20) {
                Bukkit.getServer().setMaxPlayers(20);
            }
            this.instance$dedicatedServer = Reflections.method$MinecraftServer$getServer.invoke(null);
            this.field$MinecraftServer$onlineMode.setBoolean(this.instance$dedicatedServer, true);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        if (!Bukkit.getServer().getOnlineMode()) {
            return;
        }
        this.plugin.onPluginLoad();
    }

    @Override
    public void onEnable() {
        if (!Bukkit.getServer().getOnlineMode()) {
            this.plugin.logger().warn("CraftEngine Community Edition requires online mode to be enabled.");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            this.plugin.scheduler().asyncRepeating(() -> {
                if (Bukkit.getServer().getMaxPlayers() > 20) {
                    Bukkit.getServer().setMaxPlayers(20);
                }
                try {
                    this.field$MinecraftServer$onlineMode.setBoolean(this.instance$dedicatedServer, true);
                } catch (IllegalAccessException e) {
                    this.plugin.logger().warn("Failed to run CraftEngine Community Edition", e);
                    Bukkit.shutdown();
                }
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                if (players.size() > 20) {
                    this.plugin.logger().warn("CraftEngine Community Edition restricts servers to a maximum of 20 concurrent players.");
                    this.plugin.logger().warn("Your server has exceeded this limit and will be shut down.");
                    Bukkit.shutdown();
                }
            }, 1, 1, TimeUnit.MINUTES);
            this.plugin.onPluginEnable();
            this.plugin.logger().warn("You're using the CraftEngine Community Edition");
            this.plugin.logger().warn(" - Maximum player limit is restricted to 20");
        }
    }

    @Override
    public void onDisable() {
        if (!Bukkit.getServer().getOnlineMode()) {
            return;
        }
        this.plugin.onPluginDisable();
    }
}
