package net.momirealms.craftengine.core.plugin.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface SchedulerAdapter {

    Executor async();

    PlatformExecutor platform();

    default void executeAsync(Runnable task) {
        async().execute(task);
    }

    SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit);

    SchedulerTask asyncRepeating(Runnable task, long delay, long interval, TimeUnit unit);

    SchedulerTask asyncRepeating(Consumer<SchedulerTask> task, long delay, long interval, TimeUnit unit);

    void shutdownScheduler();

    void shutdownExecutor();
}
