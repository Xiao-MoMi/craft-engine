package net.momirealms.craftengine.core.plugin.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface SchedulerAdapter<W> {

    Executor async();

    RegionExecutor<W> sync();

    default void executeAsync(Runnable task) {
        async().execute(task);
    }

    default void executeSync(Runnable task, W world, int x, int z) {
        sync().run(task, world, x, z);
    }

    default void executeSync(Runnable task) {
        sync().run(task, null, 0, 0);
    }

    SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit);

    SchedulerTask asyncRepeating(Runnable task, long delay, long interval, TimeUnit unit);

    void shutdownScheduler();

    void shutdownExecutor();
}
