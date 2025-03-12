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
