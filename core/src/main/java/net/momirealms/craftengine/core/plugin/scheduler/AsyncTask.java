package net.momirealms.craftengine.core.plugin.scheduler;

import java.util.concurrent.ScheduledFuture;

public class AsyncTask implements SchedulerTask {
    private final ScheduledFuture<?> future;

    public AsyncTask(ScheduledFuture<?> future) {
        this.future = future;
    }

    @Override
    public void cancel() {
        future.cancel(false);
    }

    @Override
    public boolean cancelled() {
        return future.isCancelled();
    }
}
