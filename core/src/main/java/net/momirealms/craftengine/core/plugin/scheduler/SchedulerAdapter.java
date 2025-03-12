/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
