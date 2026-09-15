package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Signal<T> permits MutableSignal, AsyncSignal, AbstractSignal, ListSignal, SetSignal, MapSignal {

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

    /**
     * 防抖, 上游每失效一次就把通知推后 {@code ticks} 个 tick, 连续失效只在最后一次之后通知一次.
     * <p>有订阅期间 {@link #get()} 返回上一次通知时读取的值, 等待期间仍能读到旧值. 没有订阅时透传上游,
     * 也不占用调度任务. 通知由 CraftEngine 的平台调度器发出.
     *
     * @param ticks 静默多少 tick 后通知, 必须为正
     * @return 防抖后的 Signal
     * @throws IllegalArgumentException {@code ticks} 小于等于 0
     */
    @NotNull
    Signal<T> debounce(long ticks);

    /**
     * 按毫秒防抖, 延时任务由 CraftEngine 的异步调度器执行, 其余值语义见 {@link #debounce(long)}.
     * <p>失效通知在异步工作线程发出, 订阅回调必须线程安全.
     *
     * @param millis 静默多少毫秒后通知, 必须为正
     * @return 防抖后的 Signal
     * @throws IllegalArgumentException {@code millis} 小于等于 0
     */
    @NotNull
    Signal<T> debounceMillis(long millis);

    /**
     * 节流, 两次通知之间至少间隔 {@code ticks} 个 tick.
     * <p>间隔已经满足时立即通知. 窗口内的多次失效合并为到点时的一次尾沿通知, 新窗口从尾沿通知开始计算.
     * 有订阅时保持上一次通知的值, 无订阅时透传上游.
     *
     * @param ticks 两次通知之间至少间隔多少 tick, 必须为正
     * @return 节流后的 Signal
     * @throws IllegalArgumentException {@code ticks} 小于等于 0
     */
    @NotNull
    Signal<T> throttle(long ticks);

    /**
     * 按毫秒节流, 尾沿任务由 CraftEngine 的异步调度器执行, 其余值语义见 {@link #throttle(long)}.
     *
     * @param millis 两次通知之间至少间隔多少毫秒, 必须为正
     * @return 节流后的 Signal
     * @throws IllegalArgumentException {@code millis} 小于等于 0
     */
    @NotNull
    Signal<T> throttleMillis(long millis);

    /**
     * 创建异步数据源. {@link #get()} 立即返回占位值或最近完成的值, 创建时调度一次首载.
     * <p>装载完成后在执行器线程发布新值并发送失效. 装载失败和执行器拒绝由 CraftEngine logger 上报,
     * 读取仍返回最近一次成功结果.
     *
     * @param <T> 值类型
     * @param placeholder 首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行重算的执行器
     * @param loader 重算函数, 必须线程安全且不得使本 Signal 失效
     * @return 异步 Signal
     */
    @NotNull
    static <T> AsyncSignal<T> async(T placeholder, @NotNull Executor executor, @NotNull Supplier<? extends T> loader) {
        return async(placeholder, executor, loader, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建使用自定义判等函数的异步数据源. 判等函数在装载完成线程执行, 相同结果不发送失效.
     *
     * @param <T> 值类型
     * @param placeholder 首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行重算的执行器
     * @param loader 重算函数
     * @param sameValue 判等函数
     * @return 异步 Signal
     */
    @NotNull
    static <T> AsyncSignal<T> async(T placeholder, @NotNull Executor executor, @NotNull Supplier<? extends T> loader, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        AsyncSignalImpl<T> signal = new AsyncSignalImpl<>(placeholder, executor, loader, sameValue, null);
        signal.scheduleInitialLoad();
        return signal;
    }

    /**
     * 创建按 tick 轮询的异步数据源. 有订阅期间每 {@code periodTicks} 个 tick 重载一次, 无订阅时停表.
     * <p>创建时调度首载. 重新订阅时, 若上次装载结束已经超过一个周期, 会立即补载一次.
     *
     * @param <T> 值类型
     * @param placeholder 首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行重算的执行器
     * @param loader 重算函数
     * @param periodTicks 轮询周期, 必须为正
     * @return 轮询异步 Signal
     * @throws IllegalArgumentException {@code periodTicks} 小于等于 0
     */
    @NotNull
    static <T> AsyncSignal<T> polling(T placeholder, @NotNull Executor executor, @NotNull Supplier<? extends T> loader, long periodTicks) {
        return polling(placeholder, executor, loader, periodTicks, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建使用自定义判等函数的 tick 轮询数据源.
     *
     * @param <T> 值类型
     * @param placeholder 首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行重算的执行器
     * @param loader 重算函数
     * @param periodTicks 轮询周期, 必须为正
     * @param sameValue 判等函数
     * @return 轮询异步 Signal
     * @throws IllegalArgumentException {@code periodTicks} 小于等于 0
     */
    @NotNull
    static <T> AsyncSignal<T> polling(T placeholder, @NotNull Executor executor, @NotNull Supplier<? extends T> loader, long periodTicks, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        AsyncSignalImpl<T> signal = new AsyncSignalImpl<>(placeholder, executor, loader, sameValue, AsyncSignalImpl.Polling.everyTicks(periodTicks));
        signal.scheduleInitialLoad();
        return signal;
    }

    /**
     * 创建按毫秒轮询的异步数据源. 时钟由 CraftEngine 的异步调度器驱动, 最小周期为 50 毫秒.
     *
     * @param <T> 值类型
     * @param placeholder 首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行重算的执行器
     * @param loader 重算函数
     * @param periodMillis 轮询周期毫秒数, 不小于 50
     * @return 轮询异步 Signal
     * @throws IllegalArgumentException {@code periodMillis} 小于 50
     */
    @NotNull
    static <T> AsyncSignal<T> pollingMillis(T placeholder, @NotNull Executor executor, @NotNull Supplier<? extends T> loader, long periodMillis) {
        return pollingMillis(placeholder, executor, loader, periodMillis, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建使用自定义判等函数的毫秒轮询数据源.
     *
     * @param <T> 值类型
     * @param placeholder 首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行重算的执行器
     * @param loader 重算函数
     * @param periodMillis 轮询周期毫秒数, 不小于 50
     * @param sameValue 判等函数
     * @return 轮询异步 Signal
     * @throws IllegalArgumentException {@code periodMillis} 小于 50
     */
    @NotNull
    static <T> AsyncSignal<T> pollingMillis(T placeholder, @NotNull Executor executor, @NotNull Supplier<? extends T> loader, long periodMillis, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        AsyncSignalImpl<T> signal = new AsyncSignalImpl<>(placeholder, executor, loader, sameValue, AsyncSignalImpl.Polling.everyMillis(periodMillis));
        signal.scheduleInitialLoad();
        return signal;
    }
}
