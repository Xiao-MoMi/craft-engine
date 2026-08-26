package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

public sealed interface Signal<T> permits MutableSignal, AbstractSignal {

    /**
     * 读取当前值.
     *
     * @return 当前值
     */
    T get();

    /**
     * 订阅后续失效, 订阅时不会补发当前状态.
     * <p>回调在触发失效的线程同步执行, 同一个回调可能被多个写入线程并发调用.
     * <strong>回调必须线程安全, 且不得直接或间接使同一个 Signal 再次失效</strong>.
     * <p>Signal 弱持有订阅节点. <strong>调用方必须保存返回的凭证</strong>, 凭证被回收后订阅会自动结束.
     *
     * @param listener 失效回调
     * @return 可用于提前退订的凭证
     */
    @NotNull
    Subscription onDirty(@NotNull Runnable listener);

    /**
     * 创建使用 {@link Objects#equals(Object, Object)} 判等的可写数据源.
     *
     * @param <T> 值类型
     * @param initial 初始值, 允许为 {@code null}
     * @return 可写 Signal
     */
    @NotNull
    static <T> MutableSignal<T> of(T initial) {
        return new MutableSignalImpl<>(initial);
    }

    /**
     * 创建使用指定判等函数的可写数据源.
     * <p><strong>判等函数只接收两个非 {@code null} 值</strong>, 并且必须廉价、无副作用且满足自反性.
     * Signal 会在整个生命周期内持有这个函数, 不应捕获 Player、World、Window 等短生命周期对象.
     *
     * @param <T> 值类型
     * @param initial 初始值, 允许为 {@code null}
     * @param sameValue 判等函数
     * @return 可写 Signal
     */
    @NotNull
    static <T> MutableSignal<T> of(T initial, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new MutableSignalImpl<>(initial, sameValue);
    }

    /**
     * 惰性映射当前值, 上游失效会原样传给下游.
     * <p>{@code mapper} 只在派生值被读取时执行并按上游版本缓存. 并发读取可能重复计算同一版本,
     * 因此函数必须无副作用.
     *
     * @param <R> 派生值类型
     * @param mapper 映射函数
     * @return 派生 Signal
     */
    @NotNull
    <R> Signal<R> map(@NotNull Function<? super T, ? extends R> mapper);

    /**
     * 映射当前值, 只有派生值真正变化时才向下游发送失效.
     * <p>有订阅者时, 上游每次失效都会立即计算和判等；没有订阅者时由下一次读取触发计算.
     * 计算不持锁且可能重复执行, {@code mapper} 必须廉价且无副作用.
     *
     * @param <R> 派生值类型
     * @param mapper 映射函数
     * @return 会截断重复值的派生 Signal
     */
    @NotNull
    <R> Signal<R> mapDistinct(@NotNull Function<? super T, ? extends R> mapper);

    /**
     * 使用指定判等函数映射当前值并截断重复结果.
     * <p>判等函数与 mapper 都可能在失效线程和读取线程执行, 必须廉价且无副作用.
     *
     * @param <R> 派生值类型
     * @param mapper 映射函数
     * @param sameValue 派生值判等函数
     * @return 会截断重复值的派生 Signal
     */
    @NotNull
    <R> Signal<R> mapDistinct(@NotNull Function<? super T, ? extends R> mapper, @NotNull BiPredicate<? super R, ? super R> sameValue);
}
