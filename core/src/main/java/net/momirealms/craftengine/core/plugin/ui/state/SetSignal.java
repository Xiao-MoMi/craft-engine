package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 同时实现 {@link Set} 与 {@link Signal} 的集合装饰器. 有效变更落地后发送失效, {@link #get()} 返回包装器自身的活视图.
 * <p>线程安全由被包装的 {@code Set} 决定, 判等使用包装器身份. 派生函数需要返回不可变结果.
 * <p><strong>包装器会长期持有 delegate 和元素, 禁止存放 {@code Player}、{@code Entity}、{@code World}.</strong>
 *
 * @param <E> 元素类型
 */
public sealed interface SetSignal<E> extends Signal<Set<E>>, Set<E> permits SetSignalImpl {

    /**
     * 挂一个元素钩子, 元素存入<strong>之前</strong>调用, 返回值才是真正存进去的. 生命周期与顺序见 {@link ListSignal#beforeAdd}.
     * <p>{@code add} 先用原元素查重, 已有就不跑钩子. 钩子换出的元素若与已有元素判等, 这次放入会落空.
     * <p><strong>只弱持有钩子, 调用方必须保存返回的凭证</strong>, 寿命见 {@link ListSignal#beforeAdd}.
     *
     * @param hook 收到调用方要放的元素, 返回真正存进去的
     * @return 钩子凭证, 关闭即摘除这个钩子
     */
    @NotNull
    Subscription beforeAdd(@NotNull Function<? super E, ? extends E> hook);

    /**
     * 挂一个元素钩子, 元素移除<strong>之后</strong>调用. 异常与通知语义见 {@link ListSignal#afterRemove}.
     * <p>寿命同 {@link #beforeAdd}: 只弱持有钩子, 调用方必须保存凭证.
     *
     * @param hook 收到被移除的元素
     * @return 钩子凭证, 关闭即摘除这个钩子
     */
    @NotNull
    Subscription afterRemove(@NotNull Consumer<? super E> hook);

    /**
     * 把 {@code changes} 期间本线程对本集合的变更合并成一次通知, 语义见 {@link ListSignal#batch}.
     *
     * @param changes 要合并的一批变更
     */
    void batch(@NotNull Runnable changes);

    /**
     * 包一个现成的 {@code Set}. 之后<strong>只能经包装器改它</strong>, 绕过包装器直接改 delegate 不会通知任何人.
     *
     * @param <E> 元素类型
     * @param delegate 被包装的 {@code Set}
     * @return 包装器
     */
    @NotNull
    static <E> SetSignal<E> wrap(@NotNull Set<E> delegate) {
        return new SetSignalImpl<>(delegate);
    }

    /**
     * 新建一个包着 {@link CopyOnWriteArraySet} 的装饰器, 支持写入期间的并发迭代.
     * <p>写时复制每次写都复制整个数组, 且 {@code contains} 是线性的, 热路径或大集合要按自己的访问模式另选 delegate 用 {@link #wrap}.
     *
     * @param <E> 元素类型
     * @return 包装器
     */
    @NotNull
    static <E> SetSignal<E> of() {
        return wrap(new CopyOnWriteArraySet<>());
    }
}
