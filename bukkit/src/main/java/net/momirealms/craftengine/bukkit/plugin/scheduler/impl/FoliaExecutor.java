package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.plugin.scheduler.RegionExecutor;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FoliaExecutor implements RegionExecutor<World> {
    private final BukkitCraftEngine plugin;

    public FoliaExecutor(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run(Runnable runnable, World world, int x, int z) {
        Optional.ofNullable(world).ifPresentOrElse(w ->
                Bukkit.getRegionScheduler().execute(plugin.javaPlugin(), w, x, z, runnable),
                () -> Bukkit.getGlobalRegionScheduler().execute(plugin.javaPlugin(), runnable)
        );
    }

    @Override
    public void runDelayed(Runnable runnable, World world, int x, int z) {
        run(runnable, world, x, z);
    }

    @Override
    public SchedulerTask runAsyncRepeating(Runnable runnable, long delay, long period) {
        return runRepeating(runnable, delay, period, null, 0, 0);
    }

    @Override
    public SchedulerTask runAsyncLater(Runnable runnable, long delay) {
        return runLater(runnable, delay, null, 0, 0);
    }

    @Override
    public SchedulerTask runLater(Runnable runnable, long delay, World world, int x, int z) {
        if (world == null) {
            if (delay <= 0) {
                return new FoliaTask(Bukkit.getGlobalRegionScheduler().run(plugin.javaPlugin(), scheduledTask -> runnable.run()));
            } else {
                return new FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin.javaPlugin(), scheduledTask -> runnable.run(), delay));
            }
        } else {
            if (delay <= 0) {
                return new FoliaTask(Bukkit.getRegionScheduler().run(plugin.javaPlugin(), world, x, z, scheduledTask -> runnable.run()));
            } else {
                return new FoliaTask(Bukkit.getRegionScheduler().runDelayed(plugin.javaPlugin(), world, x, z, scheduledTask -> runnable.run(), delay));
            }
        }
    }

    @Override
    public SchedulerTask runRepeating(Runnable runnable, long delay, long period, World world, int x, int z) {
        if (world == null) {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin.javaPlugin(), scheduledTask -> runnable.run(), delay, period));
        } else {
            return new FoliaTask(Bukkit.getRegionScheduler().runAtFixedRate(plugin.javaPlugin(), world, x, z, scheduledTask -> runnable.run(), delay, period));
        }
    }

    @Override
    public void execute(@NotNull Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(plugin.javaPlugin(), runnable);
    }
}
