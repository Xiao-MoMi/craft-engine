package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

/**
 * 保存一个可拉取的值, 并在值可能变化时发送失效通知.
 *
 * @param <T> 值类型, 允许为 {@code null}
 */
public interface Signal<T> {

    /**
     * 读取当前值.
     *
     * @return 当前值, 可以为 {@code null}
     */
    T get();

    /**
     * 订阅后续失效, 订阅时不会补发当前状态.
     *
     * @param listener 失效回调
     * @return 可用于提前退订的凭证
     */
    @NotNull
    Subscription onDirty(@NotNull Runnable listener);
}
