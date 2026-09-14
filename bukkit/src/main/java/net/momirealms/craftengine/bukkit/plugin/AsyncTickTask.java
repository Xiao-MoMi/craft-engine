package net.momirealms.craftengine.bukkit.plugin;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncTickTask implements Runnable {
    private final BukkitCraftEngine plugin;
    private final AtomicBoolean running = new AtomicBoolean();

    AsyncTickTask(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!this.running.compareAndSet(false, true)) return;
        try {
            for (BukkitServerPlayer player : this.plugin.networkManager().onlineUsers()) {
                player.asyncTick();
            }
        } finally {
            this.running.set(false);
        }
    }
}
