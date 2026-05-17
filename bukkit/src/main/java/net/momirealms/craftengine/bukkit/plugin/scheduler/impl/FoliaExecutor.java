package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public final class FoliaExecutor extends AbstractBukkitExecutor {
    private final BukkitCraftEngine plugin;

    public FoliaExecutor(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Runnable r) {
        Bukkit.getGlobalRegionScheduler().execute(this.plugin.javaPlugin(), r);
    }

    @Override
    public void run(Runnable r, Runnable retired, Entity entity) {
        entity.getScheduler().run(this.plugin.javaPlugin(), t -> r.run(), retired);
    }

    @Override
    public void run(Runnable r, World world, int x, int z) {
        if (world == null) {
            execute(r);
        } else {
            Bukkit.getRegionScheduler().execute(this.plugin.javaPlugin(), world, x, z, r);
        }
    }

    @Override
    public void runDelayed(Runnable r, World world, int x, int z) {
        run(r, world, x, z);
    }

    @Override
    public void runDelayed(Runnable r, Runnable retired, Entity entity) {
        entity.getScheduler().run(this.plugin.javaPlugin(), t -> r.run(), retired);
    }

    @Override
    public SchedulerTask runLater(Runnable r, long delay, World world, int x, int z) {
        if (world == null) {
            if (delay <= 0) {
                return new FoliaTask(Bukkit.getGlobalRegionScheduler().run(this.plugin.javaPlugin(), scheduledTask -> r.run()));
            } else {
                return new FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin.javaPlugin(), scheduledTask -> r.run(), delay));
            }
        } else {
            if (delay <= 0) {
                return new FoliaTask(Bukkit.getRegionScheduler().run(this.plugin.javaPlugin(), world, x, z, scheduledTask -> r.run()));
            } else {
                return new FoliaTask(Bukkit.getRegionScheduler().runDelayed(this.plugin.javaPlugin(), world, x, z, scheduledTask -> r.run(), delay));
            }
        }
    }

    @Override
    public SchedulerTask runLater(Runnable r, Runnable retired, long delay, Entity entity) {
        if (delay <= 0) {
            return new FoliaTask(entity.getScheduler().run(this.plugin.javaPlugin(), (t) -> r.run(), retired));
        } else {
            return new FoliaTask(entity.getScheduler().runDelayed(this.plugin.javaPlugin(), (t) -> r.run(), retired, delay));
        }
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, long delay, long period, World world, int x, int z) {
        if (world == null) {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.plugin.javaPlugin(), scheduledTask -> r.run(), delay, period));
        } else {
            return new FoliaTask(Bukkit.getRegionScheduler().runAtFixedRate(this.plugin.javaPlugin(), world, x, z, scheduledTask -> r.run(), delay, period));
        }
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, Runnable retired, long delay, long period, Entity entity) {
        return new FoliaTask(entity.getScheduler().runAtFixedRate(this.plugin.javaPlugin(), (t) -> r.run(), retired, delay, period));
    }
}
