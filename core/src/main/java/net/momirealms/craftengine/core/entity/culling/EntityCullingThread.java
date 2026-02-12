package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.logger.Debugger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntityCullingThread {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final int id;
    private final int threads;
    private int timer;

    public EntityCullingThread(int id, int threads) {
        this.id = id;
        this.threads = threads;
    }

    public void start() {
        // 错开线程启动时间，避免所有线程同时执行
        long initialDelay = this.id * (50L / this.threads);
        this.scheduler.scheduleAtFixedRate(this::scheduleTask, initialDelay, 50, TimeUnit.MILLISECONDS);
    }

    private void scheduleTask() {
        // 使用CAS操作，更安全
        if (!this.isRunning.compareAndSet(false, true)) {
            return;
        }

        this.scheduler.execute(() -> {
            try {
                int processed = 0;
                long startTime = System.nanoTime();

                for (Player player : CraftEngine.instance().networkManager().onlineUsers()) {
                    // 使用位运算确保非负，使用 threads 而不是 threads-1 确保均匀分布
                    if ((player.uuid().hashCode() & 0x7FFFFFFF) % this.threads == this.id) {
                        player.entityCullingTick();
                        processed++;
                    }
                }

                long duration = System.nanoTime() - startTime;
                if (Config.debugEntityCulling() && this.timer++ % 20 == 0) {
                    String value = String.format("EntityCullingThread-%d processed %d players in %sms",
                            this.id, processed, String.format("%.2f", duration / 1_000_000.0));
                    Debugger.ENTITY_CULLING.debug(() -> value);
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().severe("Failed to run entity culling tick", t);
            } finally {
                this.isRunning.set(false);
            }
        });
    }

    public void stop() {
        this.scheduler.shutdown();
    }
}