package net.momirealms.craftengine.core.plugin.scheduler;

import java.util.concurrent.Executor;

public interface RegionExecutor<W> extends Executor {

    void run(Runnable runnable, W world, int x, int z);

    default void run(Runnable runnable) {
        run(runnable, null, 0, 0);
    }

    void runDelayed(Runnable runnable, W world, int x, int z);

    default void runDelayed(Runnable runnable) {
        runDelayed(runnable, null, 0, 0);
    }

    SchedulerTask runAsyncRepeating(Runnable runnable, long delay, long period);

    SchedulerTask runAsyncLater(Runnable runnable, long delay);

    SchedulerTask runLater(Runnable runnable, long delay, W world, int x, int z);

    SchedulerTask runRepeating(Runnable runnable, long delay, long period, W world, int x, int z);

    default SchedulerTask runRepeating(Runnable runnable, long delay, long period) {
        return runRepeating(runnable, delay, period, null, 0, 0);
    }
}
