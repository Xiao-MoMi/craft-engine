package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.plugin.scheduler.PlatformExecutor;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.bukkit.Location;
import org.bukkit.World;

public abstract class AbstractBukkitExecutor implements PlatformExecutor {

    @Override
    public void run(Runnable r, net.momirealms.craftengine.core.world.World world, int x, int z) {
        run(r, world == null ? null : (World) world.platformWorld(), x, z);
    }

    public void run(Runnable r, Location location) {
        run(r, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public abstract void run(Runnable r, World world, int x, int z);

    @Override
    public void run(Runnable r, Runnable retired, Entity entity) {
        org.bukkit.entity.Entity platformEntity = (org.bukkit.entity.Entity) entity.platformEntity();
        run(r, retired, platformEntity);
    }

    public abstract void run(Runnable r, Runnable retired, org.bukkit.entity.Entity entity);

    @Override
    public void runDelayed(Runnable r, net.momirealms.craftengine.core.world.World world, int x, int z) {
        runDelayed(r, world == null ? null : (World) world.platformWorld(), x, z);
    }

    public void runDelayed(Runnable r, Location location) {
        runDelayed(r, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public abstract void runDelayed(Runnable r, World world, int x, int z);

    @Override
    public void runDelayed(Runnable r, Runnable retired, Entity entity) {
        runDelayed(r, retired, (org.bukkit.entity.Entity) entity.platformEntity());
    }

    public abstract void runDelayed(Runnable r, Runnable retired, org.bukkit.entity.Entity entity);

    @Override
    public SchedulerTask runLater(Runnable r, long delay, net.momirealms.craftengine.core.world.World world, int x, int z) {
        return runLater(r, delay, world == null ? null : (World) world.platformWorld(), 0, 0);
    }

    @Override
    public SchedulerTask runLater(Runnable r, Runnable retired, long delay, Entity entity) {
        return runLater(r, retired, delay, (org.bukkit.entity.Entity) entity.platformEntity());
    }

    public SchedulerTask runLater(Runnable r, long delay, Location location) {
        return runLater(r, delay, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public abstract SchedulerTask runLater(Runnable r, long delay, World world, int x, int z);

    public abstract SchedulerTask runLater(Runnable r, Runnable retired, long delay, org.bukkit.entity.Entity entity);

    @Override
    public SchedulerTask runRepeating(Runnable r, long delay, long period, net.momirealms.craftengine.core.world.World world, int x, int z) {
        return runRepeating(r, delay, period, world == null ? null : (World) world.platformWorld(), x, z);
    }

    public SchedulerTask runRepeating(Runnable r, long delay, long period, Location location) {
        return runRepeating(r, delay, period, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    @Override
    public SchedulerTask runRepeating(Runnable r, Runnable retired, long delay, long period, Entity entity) {
        return runRepeating(r, retired, delay, period, (org.bukkit.entity.Entity) entity.platformEntity());
    }

    public abstract SchedulerTask runRepeating(Runnable r, long delay, long period, World world, int x, int z);

    public abstract SchedulerTask runRepeating(Runnable r, Runnable retired, long delay, long period, org.bukkit.entity.Entity entity);
}
