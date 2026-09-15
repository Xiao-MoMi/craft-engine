package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 同时实现 {@link List} 与 {@link Signal} 的集合装饰器. 有效变更落地后发送失效, {@link #get()} 返回包装器自身的活视图.
 * <p>线程安全由被包装的 {@code List} 决定, 判等使用包装器身份. 派生函数需要返回不可变结果, 不能直接返回这个活视图.
 * {@code sort} 在元素不少于两个时采用保守通知, 即使顺序没有变化也会发送一次失效.
 * <p><strong>包装器会长期持有 delegate 和元素, 禁止存放 {@code Player}、{@code Entity}、{@code World}.</strong>
 *
 * <pre>{@code
 * ListSignal<String> rows = ListSignal.of();
 * rows.batch(() -> {
 *     rows.clear();
 *     rows.addAll(List.of("first", "second"));
 * });
 * Signal<Integer> count = rows.mapDistinct(List::size);
 * }</pre>
 *
 * @param <E> 元素类型
 */
public sealed interface ListSignal<E> extends Signal<List<E>>, List<E> permits ListSignalImpl {

    /**
     * 挂一个元素钩子, 元素存入<strong>之前</strong>调用, 返回值才是真正存进去的, 原样返回就是不换.
     * <p>钩子在写入线程同步执行, 多个钩子按注册顺序串联, 前一个返回值会传给后一个.
     * 替换类操作({@code set}、{@code ListIterator.set})先对旧元素执行 {@link #afterRemove} 钩子, 再处理新元素,
     * 让按位置维护的旁表先释放旧记录. {@code set} 只在新旧是同一个对象时整体跳过钩子, 判据是身份而不是 {@code equals}.
     * <p>钩子替换元素后, 调用方再用原对象执行 {@code remove(Object)} 或 {@code contains} 可能找不到已存元素.
     * <p><strong>本 signal 只弱持有钩子, 调用方必须保存返回的凭证</strong>, 凭证被回收或关闭后钩子随即失效.
     * 钩子不得建立会写回本 signal 的订阅.
     *
     * @param hook 收到调用方要放的元素, 返回真正存进去的
     * @return 钩子凭证, 关闭即摘除这个钩子
     */
    @NotNull
    Subscription beforeAdd(@NotNull Function<? super E, ? extends E> hook);

    /**
     * 挂一个元素钩子, 元素从集合移除<strong>之后</strong>调用. 按下标、按迭代器移除时收到的是被存着的那个.
     * {@code remove(Object)} 只能给调用方传入的参数, 对身份判等的元素类型两者是同一个;
     * 元素被 {@link #beforeAdd} 换过时这里收到的也是调用方给的那个, 不是集合里存着的那个.
     * <p>钩子抛出时变更已经落地, 异常会抛给写入方, 订阅者仍会收到这次失效.
     * <p>寿命与 {@link #beforeAdd} 相同: 只弱持有钩子, 调用方必须保存凭证.
     *
     * @param hook 收到被移除的元素
     * @return 钩子凭证, 关闭即摘除这个钩子
     */
    @NotNull
    Subscription afterRemove(@NotNull Consumer<? super E> hook);

    /**
     * 把 {@code changes} 期间本线程对本集合的变更合并成一次通知, 嵌套时只有最外层通知.
     * <p>只合并本线程的变更, 别的线程这期间的变更照常各自通知. {@code changes} 抛出时已经落地的变更保留并仍通知一次.
     * 两边都抛出时, 最终传播通知阶段的异常.
     *
     * @param changes 要合并的一批变更
     */
    void batch(@NotNull Runnable changes);

    /**
     * 包一个现成的 {@code List}. 之后<strong>只能经包装器改它</strong>, 绕过包装器直接改 delegate 不会通知任何人.
     *
     * @param <E> 元素类型
     * @param delegate 被包装的 {@code List}
     * @return 包装器
     */
    @NotNull
    static <E> ListSignal<E> wrap(@NotNull List<E> delegate) {
        return new ListSignalImpl<>(delegate);
    }

    /**
     * 新建一个包着 {@link CopyOnWriteArrayList} 的装饰器, 支持写入期间的并发迭代.
     * <p>写时复制每次写都复制整个数组, 热路径或大集合要按自己的访问模式另选 delegate 用 {@link #wrap}.
     *
     * @param <E> 元素类型
     * @return 包装器
     */
    @NotNull
    static <E> ListSignal<E> of() {
        return wrap(new CopyOnWriteArrayList<>());
    }
}
