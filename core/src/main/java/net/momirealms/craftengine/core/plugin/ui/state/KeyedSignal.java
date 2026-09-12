package net.momirealms.craftengine.core.plugin.ui.state;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 按 key 保存互相独立的值, 每个分区单独读取、失效和订阅.
 * <p><strong>{@code K} 禁止使用 {@code Player} 一类与在线会话绑定的重对象</strong>. 玩家维度请使用 {@link PlayerKeyedSignal}.
 *
 * @param <K> 分区 key 类型
 * @param <T> 值类型, 允许为 {@code null}
 */
public sealed interface KeyedSignal<K, T> permits MutableKeyedSignal, PlayerKeyedSignal, AbstractKeyedSignal {

    /**
     * 读取指定分区的当前值.
     *
     * @param key 分区 key
     * @return 分区当前值
     */
    T get(@NotNull K key);

    /**
     * 声明指定分区的当前值已经过期. 尚未创建的分区不会因此建立或装载.
     * <p>同步来源当场发送失效, 并在下次 {@link #get} 时重算. 异步来源立即调度后台重载,
     * 结果与当前值不同时再发送失效.
     *
     * @param key 分区 key
     */
    void dirty(@NotNull K key);

    /**
     * 将当前已经创建的全部分区标脏, 后续效果与逐个调用 {@link #dirty(Object)} 相同.
     */
    void dirtyAll();

    /**
     * 返回指定分区的稳定句柄. 分区被 {@link #remove(Object)} 并在后续访问中重建时, 句柄会跟到新分区,
     * 已有订阅与派生仍然有效.
     * <p>句柄按弱引用缓存. 调用方或绑定仍持有句柄时, 同一个 key 会得到同一实例.
     * <p><strong>取句柄会创建分区, 但不会推动异步首载, 也不算订阅</strong>. 首载由第一次读取触发,
     * 到分区的失效转发则在句柄出现第一个订阅者时建立, 最后一个订阅者离开时关闭.
     *
     * @param key 分区 key
     * @return 分区句柄
     */
    @NotNull
    Signal<T> at(@NotNull K key);

    /**
     * 驱逐指定分区的缓存, 不向该分区的句柄发送失效通知.
     * <p>仍被持有的句柄继续有效, 下一次读取会建立新分区. 确实删掉分区时 {@link #keys()} 会失效.
     *
     * @param key 分区 key
     */
    void remove(@NotNull K key);

    /**
     * 驱逐全部分区, 只让 {@link #keys()} 失效一次. 已有分区句柄仍可在后续访问时重建各自的分区.
     */
    void clear();

    /**
     * 返回当前已有分区的 key. 创建分区和成功驱逐分区会使它失效, 分区值变化不会.
     * <p>值是顺序不定的不可修改快照. 快照按分区表版本缓存, 建行或删行后的第一次拉取才会重新复制.
     * <p><strong>这里记录的是已经创建的分区, 不能直接当作业务名单</strong>. {@link #get} 或 {@link #at} 首次访问一个 key
     * 也会让它出现在集合中. 只有业务本身通过写入创建分区时, 这份集合才适合作为名单使用.
     *
     * @return 当前分区 key 的集合
     */
    @NotNull
    Signal<Set<K>> keys();

    /**
     * 创建一个同步分区数据源. 每个分区在首次读取时由 {@code initial} 装载, 之后缓存结果.
     * <p>并发失效可能让 {@code initial} 为同一个 key 执行多次, <strong>函数必须廉价、无副作用并允许重试</strong>.
     *
     * <pre>{@code
     * MutableKeyedSignal<String, Integer> lengths = KeyedSignal.of(String::length);
     * int length = lengths.get("alpha");
     * lengths.at("alpha").set(length + 1);
     * }</pre>
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param initial 分区装载与重算函数, 在读取线程执行
     * @return 可写分区 signal
     */
    @NotNull
    static <K, T> MutableKeyedSignal<K, T> of(@NotNull Function<? super K, ? extends T> initial) {
        return new KeyedSignalImpl<>(initial);
    }

    /**
     * 创建一个同步分区数据源, 并指定判等函数, 全部分区共用同一个.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param initial 分区装载与重算函数, 约束见 {@link #of(Function)}
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 可写分区 signal
     */
    @NotNull
    static <K, T> MutableKeyedSignal<K, T> of(@NotNull Function<? super K, ? extends T> initial, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new KeyedSignalImpl<>(initial, sameValue);
    }

    /**
     * 创建一个异步分区数据源.
     * <p>每个分区在<strong>第一次被读到</strong>时调度一次首载. 取句柄不算读, 完成前 {@code get} 返回占位值.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 约束见 {@link Signal#async(Object, Executor, java.util.function.Supplier)}
     * @return 分区 signal
     */
    @NotNull
    static <K, T> KeyedSignal<K, T> async(T placeholder, @NotNull Executor executor, @NotNull Function<? super K, ? extends T> loader) {
        return async(placeholder, executor, loader, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建一个异步分区数据源, 并指定判等函数, 全部分区共用同一个.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 约束见 {@link Signal#async(Object, Executor, java.util.function.Supplier)}
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 分区 signal
     */
    @NotNull
    static <K, T> KeyedSignal<K, T> async(T placeholder, @NotNull Executor executor, @NotNull Function<? super K, ? extends T> loader, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new AsyncKeyedSignalImpl<>(placeholder, executor, loader, sameValue, null);
    }

    /**
     * 创建按分区独立启停的 tick 轮询数据源, 单个分区的值语义见 {@link Signal#polling(Object, Executor, java.util.function.Supplier, long)}.
     * <p>只有句柄存在订阅的分区会轮询, 单独取句柄不会启动时钟.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodTicks 轮询周期, 必须为正
     * @return 轮询的分区 signal
     * @throws IllegalArgumentException {@code periodTicks} 小于等于 0
     */
    @NotNull
    static <K, T> KeyedSignal<K, T> polling(T placeholder, @NotNull Executor executor, @NotNull Function<? super K, ? extends T> loader, long periodTicks) {
        return polling(placeholder, executor, loader, periodTicks, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建一个使用自定义判等函数的 tick 轮询分区数据源, 全部分区共用该函数.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodTicks 轮询周期, 必须为正
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 轮询的分区 signal
     * @throws IllegalArgumentException {@code periodTicks} 小于等于 0
     */
    @NotNull
    static <K, T> KeyedSignal<K, T> polling(T placeholder, @NotNull Executor executor, @NotNull Function<? super K, ? extends T> loader, long periodTicks, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new AsyncKeyedSignalImpl<>(placeholder, executor, loader, sameValue, AsyncSignalImpl.Polling.everyTicks(periodTicks));
    }

    /**
     * 创建按分区独立启停的毫秒轮询数据源, 时钟挂在 CraftEngine 异步调度器上.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodMillis 轮询周期毫秒数, 不小于 50
     * @return 轮询的分区 signal
     * @throws IllegalArgumentException {@code periodMillis} 小于 50
     */
    @NotNull
    static <K, T> KeyedSignal<K, T> pollingMillis(T placeholder, @NotNull Executor executor, @NotNull Function<? super K, ? extends T> loader, long periodMillis) {
        return pollingMillis(placeholder, executor, loader, periodMillis, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建一个使用自定义判等函数的毫秒轮询分区数据源, 全部分区共用该函数.
     *
     * @param <K> 分区 key 类型
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodMillis 轮询周期毫秒数, 不小于 50
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 轮询的分区 signal
     * @throws IllegalArgumentException {@code periodMillis} 小于 50
     */
    @NotNull
    static <K, T> KeyedSignal<K, T> pollingMillis(T placeholder, @NotNull Executor executor, @NotNull Function<? super K, ? extends T> loader, long periodMillis, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new AsyncKeyedSignalImpl<>(placeholder, executor, loader, sameValue, AsyncSignalImpl.Polling.everyMillis(periodMillis));
    }
}

