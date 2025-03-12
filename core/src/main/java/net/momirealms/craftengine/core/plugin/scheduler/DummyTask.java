package net.momirealms.craftengine.core.plugin.scheduler;

public class DummyTask implements SchedulerTask {

    @Override
    public void cancel() {
    }

    @Override
    public boolean cancelled() {
        return true;
    }
}
