package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * 可写的 {@link KeyedSignal}. 每个分区使用与 {@link MutableSignal} 相同的判等与原子更新规则.
 *
 * @param <K> 分区 key 类型
 * @param <T> 值类型, 允许为 {@code null}
 */
public sealed interface MutableKeyedSignal<K, T> extends KeyedSignal<K, T> permits MutablePlayerKeyedSignal, KeyedSignalImpl {

    /**
     * 返回指定分区的可写稳定句柄, 生命周期与装载语义见 {@link KeyedSignal#at}.
     * <p><strong>给已经被驱逐的 key 写入会把分区重新建出来</strong>, 它要等下一次驱逐才会消失.
     * 定时任务与采样回调不应给可能已经离线的玩家写入.
     *
     * @param key 分区 key
     * @return 可写的分区句柄
     */
    @Override
    @NotNull
    MutableSignal<T> at(@NotNull K key);

    /**
     * 写入指定分区的新值.
     *
     * @param key 分区 key
     * @param value 新值, 允许为 {@code null}
     */
    void set(@NotNull K key, T value);

    /**
     * 基于指定分区的当前值原子更新.
     * <p>发生写入争用时 {@code updater} 可能执行多次, <strong>必须无副作用并允许重试</strong>.
     *
     * @param key 分区 key
     * @param updater 根据当前值计算新值的纯函数
     */
    void update(@NotNull K key, @NotNull UnaryOperator<T> updater);
}

