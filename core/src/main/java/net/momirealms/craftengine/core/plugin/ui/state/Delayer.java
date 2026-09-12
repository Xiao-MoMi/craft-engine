package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

// 防抖与节流共用的一次性延时入口, 测试可以替换实际调度器.
interface Delayer {

    // 在 delay 个时间单位后执行一次, delay 必须为正.
    @NotNull
    Handle schedule(@NotNull Runnable task, long delay);

    // tick 时基沿用 CraftEngine 的平台调度线程.
    @NotNull
    static Delayer platformTicks() {
        return (task, delayTicks) -> {
            SchedulerTask scheduled = CraftEngine.instance().scheduler().platform().runLater(task, delayTicks);
            return scheduled::cancel;
        };
    }

    // 毫秒时基沿用 CraftEngine 的异步调度器.
    @NotNull
    static Delayer asyncMillis() {
        return (task, delayMillis) -> {
            SchedulerTask scheduled = CraftEngine.instance().scheduler().asyncLater(task, delayMillis, TimeUnit.MILLISECONDS);
            return scheduled::cancel;
        };
    }

    interface Handle {

        void cancel();
    }
}
