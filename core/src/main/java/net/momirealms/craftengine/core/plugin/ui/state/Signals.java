package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.util.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Signals {

    private Signals() {
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
