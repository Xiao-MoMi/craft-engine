package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public sealed interface MutableSignal<T> extends Signal<T> permits MutableSignalImpl, MutablePartitionHandle, LensSignal {

    /**
     * 写入新值, 与旧值相同时静默跳过.
     *
     * @param value 新值, 允许为 {@code null}
     */
    void set(T value);

    /**
     * 基于当前值原子更新.
     * <p>发生写入争用时, {@code updater} 可能执行多次. <strong>函数必须无副作用并允许重试</strong>.
     *
     * @param updater 根据当前值计算新值的纯函数
     */
    void update(@NotNull UnaryOperator<T> updater);

    /**
     * 把宿主值中的一个字段作为独立的可写 Signal 使用.
     * <p>写入会通过宿主的原子更新路径重建宿主值, 字段值没有变化时不会向 lens 下游发送失效.
     * getter 与 setter 可能在写入线程和读取线程重复执行, 必须无副作用. Lens 会长期持有宿主与这两个函数,
     * 不应在函数中捕获 Player、World、Window 等短生命周期对象.
     *
     * @param <F> 字段类型
     * @param getter 从宿主值读取字段
     * @param setter 使用新字段值重建宿主值
     * @return 字段对应的可写 Signal
     */
    @NotNull
    default <F> MutableSignal<F> lens(@NotNull Function<? super T, ? extends F> getter, @NotNull BiFunction<? super T, ? super F, ? extends T> setter) {
        return this.lens(getter, setter, AbstractSignal.defaultSameValue());
    }

    /**
     * 使用指定判等函数创建字段 lens.
     *
     * @param <F> 字段类型
     * @param getter 从宿主值读取字段
     * @param setter 使用新字段值重建宿主值
     * @param sameValue 字段判等函数
     * @return 字段对应的可写 Signal
     */
    @NotNull
    default <F> MutableSignal<F> lens(@NotNull Function<? super T, ? extends F> getter, @NotNull BiFunction<? super T, ? super F, ? extends T> setter, @NotNull BiPredicate<? super F, ? super F> sameValue) {
        return new LensSignal<>(this, getter, setter, sameValue);
    }
}
