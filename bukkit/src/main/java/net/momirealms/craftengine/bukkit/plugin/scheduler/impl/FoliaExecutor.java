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

package net.momirealms.craftengine.bukkit.plugin.scheduler.impl;

import net.momirealms.craftengine.core.plugin.scheduler.RegionExecutor;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FoliaExecutor implements RegionExecutor<World> {
    private final Plugin plugin;

    public FoliaExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run(Runnable runnable, World world, int x, int z) {
        Optional.ofNullable(world).ifPresentOrElse(w ->
                Bukkit.getRegionScheduler().execute(plugin, w, x, z, runnable),
                () -> Bukkit.getGlobalRegionScheduler().execute(plugin, runnable)
        );
    }

    @Override
    public void runDelayed(Runnable runnable, World world, int x, int z) {
        run(runnable, world, x, z);
    }

    @Override
    public SchedulerTask runAsyncRepeating(Runnable runnable, long delay, long period) {
        return runRepeating(runnable, delay, period, null, 0, 0);
    }

    @Override
    public SchedulerTask runAsyncLater(Runnable runnable, long delay) {
        return runLater(runnable, delay, null, 0, 0);
    }

    @Override
    public SchedulerTask runLater(Runnable runnable, long delay, World world, int x, int z) {
        if (world == null) {
            if (delay <= 0) {
                return new FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delay));
            } else {
                return new FoliaTask(Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run()));
            }
        } else {
            if (delay <= 0) {
                return new FoliaTask(Bukkit.getRegionScheduler().run(plugin, world, x, z, scheduledTask -> runnable.run()));
            } else {
                return new FoliaTask(Bukkit.getRegionScheduler().runDelayed(plugin, world, x, z, scheduledTask -> runnable.run(), delay));
            }
        }
    }

    @Override
    public SchedulerTask runRepeating(Runnable runnable, long delay, long period, World world, int x, int z) {
        if (world == null) {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, period));
        } else {
            return new FoliaTask(Bukkit.getRegionScheduler().runAtFixedRate(plugin, world, x, z, scheduledTask -> runnable.run(), delay, period));
        }
    }

    @Override
    public void execute(@NotNull Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
    }
}
