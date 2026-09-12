package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 以玩家 UUID 为分区维度的 {@link KeyedSignal}. 接收 {@link Player} 的重载会立即取出 UUID,
 * 玩家退出时对应分区自动驱逐.
 *
 * @param <T> 值类型, 允许为 {@code null}
 */
public sealed interface PlayerKeyedSignal<T> extends KeyedSignal<UUID, T> permits MutablePlayerKeyedSignal, PlayerKeyedSignalImpl {

    /**
     * 读取指定玩家分区的当前值.
     *
     * @param player 玩家
     * @return 玩家分区的当前值
     */
    default T get(@NotNull Player player) {
        return this.get(player.uuid());
    }

    /**
     * 将指定玩家的现有分区标脏, 具体效果见 {@link KeyedSignal#dirty(Object)}.
     *
     * @param player 玩家
     */
    default void dirty(@NotNull Player player) {
        this.dirty(player.uuid());
    }

    /**
     * 返回指定玩家分区的稳定句柄, 生命周期和装载语义见 {@link KeyedSignal#at}.
     *
     * @param player 玩家
     * @return 玩家分区句柄
     */
    @NotNull
    default Signal<T> at(@NotNull Player player) {
        return this.at(player.uuid());
    }

    /**
     * 驱逐指定玩家的分区缓存, 不向分区句柄发送失效通知.
     *
     * @param player 玩家
     */
    default void remove(@NotNull Player player) {
        this.remove(player.uuid());
    }

    /**
     * 创建一个同步的玩家分区数据源. 装载函数接收 UUID, 玩家退出时对应分区自动驱逐.
     *
     * <pre>{@code
     * MutablePlayerKeyedSignal<Long> coins = PlayerKeyedSignal.of(economy::balance);
     * long current = coins.get(player);
     * coins.set(player, current + 1L);
     * }</pre>
     *
     * @param <T> 值类型
     * @param initial 分区装载与重算函数, 约束见 {@link KeyedSignal#of(Function)}
     * @return 可写玩家分区 signal
     */
    @NotNull
    static <T> MutablePlayerKeyedSignal<T> of(@NotNull Function<? super UUID, ? extends T> initial) {
        return of(initial, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建一个同步的玩家分区数据源, 并指定全部分区共用的判等函数.
     *
     * @param <T> 值类型
     * @param initial 分区装载与重算函数, 约束见 {@link KeyedSignal#of(Function)}
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 可写玩家分区 signal
     */
    @NotNull
    static <T> MutablePlayerKeyedSignal<T> of(@NotNull Function<? super UUID, ? extends T> initial, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new MutablePlayerKeyedSignalImpl<>(initial, sameValue);
    }

    /**
     * 创建一个异步的玩家分区数据源, 玩家退出时对应分区自动驱逐.
     *
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @return 玩家分区 signal
     */
    @NotNull
    static <T> PlayerKeyedSignal<T> async(T placeholder, @NotNull Executor executor, @NotNull Function<? super UUID, ? extends T> loader) {
        return async(placeholder, executor, loader, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建一个异步的玩家分区数据源, 并指定全部分区共用的判等函数.
     *
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 玩家分区 signal
     */
    @NotNull
    static <T> PlayerKeyedSignal<T> async(T placeholder, @NotNull Executor executor, @NotNull Function<? super UUID, ? extends T> loader, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new PlayerKeyedSignalImpl<>(placeholder, executor, loader, sameValue, null);
    }

    /**
     * 创建一个轮询的玩家分区数据源, 玩家退出时对应分区自动驱逐.
     * <p>只有句柄被订阅了的玩家在轮询, 退出驱逐之后这名玩家的轮询随分区一起停.
     *
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodTicks 轮询周期, 必须为正
     * @return 轮询的玩家分区 signal
     * @throws IllegalArgumentException {@code periodTicks} 小于等于 0
     */
    @NotNull
    static <T> PlayerKeyedSignal<T> polling(T placeholder, @NotNull Executor executor, @NotNull Function<? super UUID, ? extends T> loader, long periodTicks) {
        return polling(placeholder, executor, loader, periodTicks, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建一个使用自定义判等函数的 tick 轮询玩家数据源.
     *
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodTicks 轮询周期, 必须为正
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 轮询的玩家分区 signal
     * @throws IllegalArgumentException {@code periodTicks} 小于等于 0
     */
    @NotNull
    static <T> PlayerKeyedSignal<T> polling(T placeholder, @NotNull Executor executor, @NotNull Function<? super UUID, ? extends T> loader, long periodTicks, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new PlayerKeyedSignalImpl<>(placeholder, executor, loader, sameValue, AsyncSignalImpl.Polling.everyTicks(periodTicks));
    }

    /**
     * 创建按毫秒轮询的玩家分区数据源, 时钟挂在 CraftEngine 异步调度器上.
     *
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodMillis 轮询周期毫秒数, 不小于 50
     * @return 轮询的玩家分区 signal
     * @throws IllegalArgumentException {@code periodMillis} 小于 50
     */
    @NotNull
    static <T> PlayerKeyedSignal<T> pollingMillis(T placeholder, @NotNull Executor executor, @NotNull Function<? super UUID, ? extends T> loader, long periodMillis) {
        return pollingMillis(placeholder, executor, loader, periodMillis, AbstractSignal.defaultSameValue());
    }

    /**
     * 创建一个使用自定义判等函数的毫秒轮询玩家数据源.
     *
     * @param <T> 值类型
     * @param placeholder 每个分区首载完成前的占位值, 允许为 {@code null}
     * @param executor 执行装载的执行器
     * @param loader 分区装载函数, 在 executor 线程执行
     * @param periodMillis 轮询周期毫秒数, 不小于 50
     * @param sameValue 判等函数, 语义见 {@link Signal#of(Object, BiPredicate)}
     * @return 轮询的玩家分区 signal
     * @throws IllegalArgumentException {@code periodMillis} 小于 50
     */
    @NotNull
    static <T> PlayerKeyedSignal<T> pollingMillis(T placeholder, @NotNull Executor executor, @NotNull Function<? super UUID, ? extends T> loader, long periodMillis, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        return new PlayerKeyedSignalImpl<>(placeholder, executor, loader, sameValue, AsyncSignalImpl.Polling.everyMillis(periodMillis));
    }
}

