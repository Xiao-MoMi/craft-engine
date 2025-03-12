package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;

public class BukkitTask implements SchedulerTask {
    private final org.bukkit.scheduler.BukkitTask bukkitTask;

    public BukkitTask(org.bukkit.scheduler.BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }

    @Override
    public void cancel() {
        this.bukkitTask.cancel();
    }

    @Override
    public boolean cancelled() {
        return bukkitTask.isCancelled();
    }
}
