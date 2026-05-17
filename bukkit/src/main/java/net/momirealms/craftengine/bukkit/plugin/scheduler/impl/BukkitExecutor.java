package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.plugin.scheduler.DummyTask;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public final class BukkitExecutor extends AbstractBukkitExecutor {
    private final BukkitCraftEngine plugin;

    public BukkitExecutor(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Runnable r) {
        if (Bukkit.isPrimaryThread()) {
            r.run();
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin.javaPlugin(), r);
    }

    @Override
    public void run(Runnable r, net.momirealms.craftengine.core.world.World world, int x, int z) {
        execute(r);
    }

    @Override
    public void run(Runnable r, Location location) {
        execute(r);
    }

    @Override
    public void run(Runnable r, Runnable retired, net.momirealms.craftengine.core.entity.Entity entity) {
        execute(r);
    }

    @Override
    public void run(Runnable r, World world, int x, int z) {
        execute(r);
    }

    @Override
    public void run(Runnable r, Runnable retired, Entity entity) {
        execute(r);
    }

    @Override
    public void runDelayed(Runnable r, World world, int x, int z) {
        Bukkit.getScheduler().runTask(plugin.javaPlugin(), r);
    }

    @Override
    public void runDelayed(Runnable r, net.momirealms.craftengine.core.world.World world, int x, int z) {
        Bukkit.getScheduler().runTask(plugin.javaPlugin(), r);
    }

    @Override
    public void runDelayed(Runnable r, Location location) {
        Bukkit.getScheduler().runTask(plugin.javaPlugin(), r);
    }

    @Override
    public void runDelayed(Runnable r, Runnable retired, Entity entity) {
        Bukkit.getScheduler().runTask(plugin.javaPlugin(), r);
    }

    @Override
    public void runDelayed(Runnable r, Runnable retired, net.momirealms.craftengine.core.entity.Entity entity) {
        Bukkit.getScheduler().runTask(plugin.javaPlugin(), r);
    }

    @Override
    public SchedulerTask runLater(Runnable r, long delay, World world, int x, int z) {
        return runLater0(r, delay);
    }

    @Override
    public SchedulerTask runLater(Runnable r, Runnable retired, long delay, Entity entity) {
        return runLater0(r, delay);
    }

    @Override
    public SchedulerTask runLater(Runnable r, long delay, net.momirealms.craftengine.core.world.World world, int x, int z) {
        return runLater0(r, delay);
    }

    @Override
    public SchedulerTask runLater(Runnable r, Runnable retired, long delay, net.momirealms.craftengine.core.entity.Entity entity) {
        return runLater0(r, delay);
    }

    @Override
    public SchedulerTask runLater(Runnable r, long delay, Location location) {
        return runLater0(r, delay);
    }

    @NonNull
    private SchedulerTask runLater0(Runnable r, long delay) {
        if (delay <= 0) {
            if (Bukkit.isPrimaryThread()) {
                r.run();
                return new DummyTask();
            } else {
                return new BukkitTask(Bukkit.getScheduler().runTask(plugin.javaPlugin(), r));
            }
        }
        return new BukkitTask(Bukkit.getScheduler().runTaskLater(plugin.javaPlugin(), r, delay));
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, long delay, long period, World world, int x, int z) {
        return new BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin.javaPlugin(), r, delay, period));
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, Runnable retired, long delay, long period, Entity entity) {
        return new BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin.javaPlugin(), r, delay, period));
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, long delay, long period, net.momirealms.craftengine.core.world.World world, int x, int z) {
        return new BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin.javaPlugin(), r, delay, period));
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, long delay, long period, Location location) {
        return new BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin.javaPlugin(), r, delay, period));
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, Runnable retired, long delay, long period, net.momirealms.craftengine.core.entity.Entity entity) {
        return new BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin.javaPlugin(), r, delay, period));
    }
}
