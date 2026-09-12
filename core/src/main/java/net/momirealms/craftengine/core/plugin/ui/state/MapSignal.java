package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * 同时实现 {@link Map} 与 {@link Signal} 的集合装饰器. 有效变更落地后发送失效, {@link #get()} 返回包装器自身的活视图.
 * <p>线程安全由被包装的 {@code Map} 决定, 判等使用包装器身份. 三个集合视图和 {@link Map.Entry#setValue} 都会写回包装器并发送失效.
 * 派生函数需要返回不可变结果, 不能直接返回这个活视图.
 * 没有元素钩子时, 非空 {@code putAll} 采用保守通知, 即使全部映射相同也会发送一次失效.
 * <p>允许 {@code null} 值的 delegate 有一处限制. key 已映射到 {@code null} 且 {@code compute} 仍返回 {@code null} 时,
 * 底层 Map 会删除条目, 包装器无法从前后两个 {@code null} 辨认这次删除, 因而不会发送失效.
 * <p><strong>包装器会长期持有 delegate、key 与值, 禁止存放 {@code Player}、{@code Entity}、{@code World}.</strong>
 *
 * @param <K> key 类型
 * @param <V> 值类型
 */
public sealed interface MapSignal<K, V> extends Signal<Map<K, V>>, Map<K, V> permits MapSignalImpl {

    /**
     * 挂一个元素钩子, 值存入<strong>之前</strong>调用, 返回值才是真正存进去的, 原样返回就是不换.
     * <p>钩子在写入线程同步执行, 多个钩子按注册顺序串联, 前一个返回值会传给后一个.
     * 替换已有映射时先对旧值执行 {@link #afterRemove} 钩子, 再处理新值,
     * 让按 key 维护的旁表先释放旧记录.
     * <p>带钩子的 {@code put} 会先读后写, 在并发 map 上不具备按 key 的原子性. {@code compute} 一族会在 delegate 的重算函数中执行钩子,
     * 钩子可能重跑, 并且<strong>不得再次操作同一张 map</strong>.
     * <p><strong>钩子属于构造期配置, 应在发布包装器之前注册, 并且不得建立会写回本 signal 的订阅.</strong>
     *
     * <pre>{@code
     * MapSignal<String, Integer> scores = MapSignal.<String, Integer>of()
     *         .beforePut((name, score) -> Math.max(0, score));
     * }</pre>
     *
     * @param hook 收到 key 与调用方要放的值, 返回真正存进去的
     * <p><strong>只弱持有钩子, 调用方必须保存返回的凭证</strong>, 寿命见 {@link ListSignal#beforeAdd}.
     *
     * @return 钩子凭证, 关闭即摘除这个钩子
     */
    @NotNull
    Subscription beforePut(@NotNull BiFunction<? super K, ? super V, ? extends V> hook);

    /**
     * 挂一个元素钩子, 映射从 map 移除<strong>之后</strong>调用, 收到的是被存着的那个值.
     * <p>钩子抛出时变更已经落地, 异常会抛给写入方, 订阅者仍会收到这次失效.
     *
     * @param hook 收到被移除的 key 与值
     * <p>寿命同 {@link #beforePut}: 只弱持有钩子, 调用方必须保存凭证.
     *
     * @return 钩子凭证, 关闭即摘除这个钩子
     */
    @NotNull
    Subscription afterRemove(@NotNull BiConsumer<? super K, ? super V> hook);

    /**
     * 把 {@code changes} 期间本线程对本集合的变更合并成一次通知, 语义见 {@link ListSignal#batch}.
     *
     * @param changes 要合并的一批变更
     */
    void batch(@NotNull Runnable changes);

    /**
     * 包一个现成的 {@code Map}. 之后<strong>只能经包装器改它</strong>, 绕过包装器直接改 delegate 不会通知任何人.
     *
     * @param <K> key 类型
     * @param <V> 值类型
     * @param delegate 被包装的 {@code Map}
     * @return 包装器
     */
    @NotNull
    static <K, V> MapSignal<K, V> wrap(@NotNull Map<K, V> delegate) {
        return new MapSignalImpl<>(delegate);
    }

    /**
     * 新建一个包着 {@link ConcurrentHashMap} 的装饰器, 任何线程都能安全读写.
     * <p><strong>它不保迭代顺序</strong>, 也不接受 {@code null} key 或值. 需要顺序时可 {@code wrap(new LinkedHashMap<>())} 并自行管理线程安全.
     *
     * @param <K> key 类型
     * @param <V> 值类型
     * @return 包装器
     */
    @NotNull
    static <K, V> MapSignal<K, V> of() {
        return wrap(new ConcurrentHashMap<>());
    }
}
