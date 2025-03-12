package net.momirealms.craftengine.core.plugin.scheduler;

public interface SchedulerTask {

    void cancel();

    boolean cancelled();
}
