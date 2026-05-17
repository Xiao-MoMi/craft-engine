package net.momirealms.craftengine.core.plugin.scheduler;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.world.World;

import java.util.concurrent.Executor;

public interface PlatformExecutor extends Executor {

    // Run

    void run(Runnable r, World world, int x, int z);

    void run(Runnable r, Runnable retired, Entity entity);

    default void run(Runnable r) {
        run(r, null, 0, 0);
    }

    // Delayed

    void runDelayed(Runnable r, World world, int x, int z);

    void runDelayed(Runnable r, Runnable retired, Entity entity);

    default void runDelayed(Runnable r) {
        runDelayed(r, null, 0, 0);
    }

    // Later

    default SchedulerTask runLater(Runnable r, long delay) {
        return runLater(r, delay, null, 0 ,0);
    }

    SchedulerTask runLater(Runnable r, long delay, World world, int x, int z);

    SchedulerTask runLater(Runnable r, Runnable retired, long delay, Entity entity);

    // Repeating

    default SchedulerTask runRepeating(Runnable r, long delay, long period) {
        return runRepeating(r, delay, period, null, 0, 0);
    }

    SchedulerTask runRepeating(Runnable r, long delay, long period, World world, int x, int z);

    SchedulerTask runRepeating(Runnable r, Runnable retired, long delay, long period, Entity entity);
}
