package net.momirealms.craftengine.core.plugin.ui.signal;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 响应式数据源: 持有单个值, 值过期时向订阅者广播失效.
 * <p>值按 {@code equals} 判断有没有变化, 原地改掉一个可变对象再写回同一个引用会被当成没变.
 *
 * @param <T> 值类型, 允许为 {@code null}
 */
public interface Signal<T> {

    T get();

    /**
     * 订阅失效信号, <strong>不允许在回调里让同一个 signal 再次失效</strong>.
     * <p>通知不携带值, 也不触发求值. signal 弱持有监听器, <strong>订阅的存活由调用方持有的凭证决定</strong>,
     * 凭证不再被引用时订阅自动消亡并在后续派发时被剔除. 因此凭证必须存起来, 丢掉就等于退订.
     *
     * @param listener 失效监听器
     * @return 订阅凭证, <strong>必须持有</strong>, 丢弃即取消订阅
     */
    @NotNull
    Subscription onDirty(@NotNull Runnable listener);

//    /**
//     * 创建一个可写数据源.
//     *
//     * @param initial 初始值, 允许为 {@code null}
//     * @return 可写 signal
//     */
//    @NotNull
//    static <T> MutableSignal<T> of(T initial) {
//        return new MutableSignalImpl<>(initial);
//    }
//
//    /**
//     * 惰性派生, 失效原样透传, {@code mapper} 仅在派生值被拉取时执行, 并按上游版本缓存结果.
//     *
//     * @param mapper 纯函数, 可在任意线程被执行
//     * @return 派生 signal
//     */
//    @NotNull
//    <R> Signal<R> map(@NotNull Function<? super T, ? extends R> mapper);
//
//    /**
//     * 派生, 上游每次失效都会立即重算并与缓存值判等,
//     * 相等则吞掉失效不再向下游传播, 适合逐层降频分派.
//     * <p><strong>{@code mapper} 不得读取其他 signal.</strong> 本方法是整个模型里唯一在失效传播路径上
//     * 求值的节点, 求值期间持有本节点的重算锁. 需要多个来源时用 {@link #combine} 显式组合.
//     *
//     * @param mapper 纯函数, 在失效线程与拉取线程被执行
//     * @return 派生 signal
//     */
//    @NotNull
//    <R> Signal<R> mapDistinct(@NotNull Function<? super T, ? extends R> mapper);
//
//    /**
//     * 创建一个异步数据源, {@link #get()} 立即返回占位值或最近完成的值, 重算由 {@code executor} 在后台执行.
//     * <p>创建时即调度一次首载. 之后由 {@link AsyncSignal#dirty} 触发重载.
//     * <p>装载失败与执行器拒绝任务都交给统一异常处理器, 不会抛给调用方, 也不会让读取失败, 详见 {@link AsyncSignal#dirty}.
//     *
//     * @param placeholder 首载完成前的占位值, 允许为 {@code null}
//     * @param executor 执行重算的执行器
//     * @param loader 重算函数, 在 executor 线程执行, 必须线程安全; 不得(直接或间接)使本 signal 失效, 同步执行器下会构成无界递归
//     * @return 异步 signal
//     */
//    @NotNull
//    static <T> AsyncSignal<T> async(T placeholder, @NotNull Executor executor, @NotNull Supplier<? extends T> loader) {
//        AsyncSignalImpl<T> signal = new AsyncSignalImpl<>(placeholder, executor, loader);
//        signal.scheduleInitialLoad();
//        return signal;
//    }
//
//    /**
//     * 组合两个来源, 任一来源失效即失效, 值在拉取时以两个来源的快照重算.
//     *
//     * @param combiner 纯函数, 可在任意线程被执行
//     * @return 组合 signal
//     */
//    @NotNull
//    static <A, B, R> Signal<R> combine(@NotNull Signal<A> a, @NotNull Signal<B> b, @NotNull BiFunction<? super A, ? super B, ? extends R> combiner) {
//        Objects.requireNonNull(combiner, "combiner");
//        return new CombinedSignal<>(new AbstractSignal<?>[]{AbstractSignal.require(a), AbstractSignal.require(b)}, values -> {
//            @SuppressWarnings("unchecked") R result = combiner.apply((A) values[0], (B) values[1]);
//            return result;
//        });
//    }
//
//    /**
//     * 组合三个来源, 语义同 {@link #combine(Signal, Signal, BiFunction)}.
//     */
//    @NotNull
//    static <A, B, C, R> Signal<R> combine(@NotNull Signal<A> a, @NotNull Signal<B> b, @NotNull Signal<C> c, @NotNull TriFunction<? super A, ? super B, ? super C, ? extends R> combiner) {
//        Objects.requireNonNull(combiner, "combiner");
//        return new CombinedSignal<>(new AbstractSignal<?>[]{AbstractSignal.require(a), AbstractSignal.require(b), AbstractSignal.require(c)}, values -> {
//            @SuppressWarnings("unchecked") R result = combiner.apply((A) values[0], (B) values[1], (C) values[2]);
//            return result;
//        });
//    }
}
