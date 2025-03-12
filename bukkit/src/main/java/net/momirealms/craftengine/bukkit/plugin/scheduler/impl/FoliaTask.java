package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;

public class FoliaTask implements SchedulerTask {
    private final ScheduledTask task;

    public FoliaTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }

    @Override
    public boolean cancelled() {
        return task.isCancelled();
    }
}
