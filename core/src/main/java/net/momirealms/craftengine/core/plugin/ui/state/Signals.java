package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.util.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Signals {
    private static final long MIN_MILLIS_PERIOD = 50L;
    private static final WeakPeriodCache<TickingSignal> millisClocks = new WeakPeriodCache<>();

    private static volatile TickingSignal ticking;
    private static volatile Delayer tickDelayer = Delayer.platformTicks();
    private static volatile Delayer millisDelayer = Delayer.asyncMillis();

    private Signals() {
    }

    @NotNull
    static Delayer tickDelayer() {
        return tickDelayer;
    }

    @NotNull
    static Delayer millisDelayer() {
        return millisDelayer;
    }

    /**
     * 服务器 tick 源. 第一个订阅者到来时启动, 最后一个离开时停表.
     * <p>每 tick 发送一次失效, 值为有订阅者期间累计经过的 tick 数, 跨停表保持单调递增.
     *
     * @return tick 源
     */
    @NotNull
    public static Signal<Long> ticking() {
        TickingSignal current = ticking;
        if (current != null) return current;
        synchronized (Signals.class) {
            if (ticking == null) {
                ticking = new TickingSignal(TickingSignal.platformTicker());
            }
            return ticking;
        }
    }

    /**
     * 按周期降频的 tick 源, 每 {@code periodTicks} 个 tick 失效一次, 值为已经过去的周期数.
     * <p>相同周期共享同一个派生节点.
     *
     * @param periodTicks 正数 tick 周期
     * @return 降频后的 tick 源
     * @throws IllegalArgumentException {@code periodTicks} 小于等于 0
     */
    @NotNull
    public static Signal<Long> everyTicks(long periodTicks) {
        if (periodTicks <= 0) {
            throw new IllegalArgumentException("periodTicks must be positive: " + periodTicks);
        }
        if (periodTicks == 1L) return ticking();
        return ((TickingSignal) ticking()).every(periodTicks);
    }

    /**
     * 毫秒时钟, 每 {@code periodMillis} 毫秒失效一次, 跨停表保持单调递增.
     * <p>任务由 CraftEngine 的异步调度器驱动, 相同周期弱共享同一实例. 第一个订阅者到来时启动,
     * 最后一个离开时取消.
     *
     * @param periodMillis 周期毫秒数, 不小于 50
     * @return 毫秒时钟
     * @throws IllegalArgumentException 周期小于 50 毫秒
     */
    @NotNull
    public static Signal<Long> everyMillis(long periodMillis) {
        if (periodMillis < MIN_MILLIS_PERIOD) {
            throw new IllegalArgumentException("periodMillis must be at least " + MIN_MILLIS_PERIOD + ": " + periodMillis);
        }
        return millisClocks.get(periodMillis, period -> new TickingSignal(TickingSignal.asyncMillisTicker(period)));
    }

    /**
     * 组合两个来源, 任一来源失效都会使结果失效.
     * <p>两个读取不构成跨来源的原子快照, {@code combiner} 必须允许并发和重复执行.
     *
     * @param <A> 第一个来源的值类型
     * @param <B> 第二个来源的值类型
     * @param <R> 组合结果类型
     * @param a 第一个来源
     * @param b 第二个来源
     * @param combiner 组合函数
     * @return 组合后的 Signal
     */
    @NotNull
    public static <A, B, R> Signal<R> combine(@NotNull Signal<A> a, @NotNull Signal<B> b, @NotNull BiFunction<? super A, ? super B, ? extends R> combiner) {
        return new CombinedSignal<>(new AbstractSignal<?>[]{AbstractSignal.require(a), AbstractSignal.require(b)}, values -> {
            @SuppressWarnings("unchecked") R result = combiner.apply((A) values[0], (B) values[1]);
            return result;
        });
    }

    /**
     * 组合三个来源, 任一来源失效都会使结果失效.
     *
     * @param <A> 第一个来源的值类型
     * @param <B> 第二个来源的值类型
     * @param <C> 第三个来源的值类型
     * @param <R> 组合结果类型
     * @param a 第一个来源
     * @param b 第二个来源
     * @param c 第三个来源
     * @param combiner 组合函数
     * @return 组合后的 Signal
     */
    @NotNull
    public static <A, B, C, R> Signal<R> combine(@NotNull Signal<A> a, @NotNull Signal<B> b, @NotNull Signal<C> c, @NotNull TriFunction<? super A, ? super B, ? super C, ? extends R> combiner) {
        return new CombinedSignal<>(new AbstractSignal<?>[]{AbstractSignal.require(a), AbstractSignal.require(b), AbstractSignal.require(c)}, values -> {
            @SuppressWarnings("unchecked") R result = combiner.apply((A) values[0], (B) values[1], (C) values[2]);
            return result;
        });
    }

    /**
     * 按 key 在同一个分区数据源之间切换, 只持有和订阅当前分区的稳定句柄.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param source 分区数据源
     * @param key 当前选择的分区 key
     * @return 切换后的 Signal
     */
    @NotNull
    public static <K, T> Signal<T> switching(@NotNull KeyedSignal<K, T> source, @NotNull Signal<K> key) {
        return new SwitchingSignal<>(source::at, AbstractSignal.require(key));
    }

    /**
     * 按 key 在若干 Signal 之间切换, 只读取和订阅当前选中的来源.
     *
     * @param <K> key 类型
     * @param <T> 值类型
     * @param sources 每个 key 对应的来源, 不能为空
     * @param key 当前选择
     * @return 切换后的 Signal
     * @throws IllegalArgumentException 来源为空, 或当前 key 没有对应来源
     */
    @NotNull
    public static <K, T> Signal<T> switching(@NotNull Map<K, ? extends Signal<T>> sources, @NotNull Signal<K> key) {
        Map<K, ? extends Signal<T>> copied = Map.copyOf(sources);
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("sources must not be empty");
        }
        return new SwitchingSignal<>(
                selected -> {
                    Signal<T> source = copied.get(selected);
                    if (source == null) {
                        throw new IllegalArgumentException("no source for key: " + selected);
                    }
                    return source;
                },
                AbstractSignal.require(key)
        );
    }

    /**
     * 汇合集合中的动态成员, 集合内容或任一成员 Signal 失效时都发送失效.
     * <p>返回值是单调递增的失效标记, 数字本身没有业务含义. 集合迭代顺序必须稳定.
     *
     * @param <T> 集合成员类型
     * @param sources 当前成员集合
     * @param signalOf 将成员转换为失效来源的函数
     * @return 随成员变化递增的 Signal
     */
    @NotNull
    public static <T> Signal<Long> merging(@NotNull Signal<? extends Collection<? extends T>> sources, @NotNull Function<? super T, ? extends Signal<?>> signalOf) {
        return new MergingSignal<>(AbstractSignal.require(sources), signalOf);
    }
}
