package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * 可写的玩家分区数据源. 接收 {@link Player} 的入口立即换成 UUID, 写入规则与 {@link MutableKeyedSignal} 相同.
 *
 * @param <T> 值类型, 允许为 {@code null}
 */
public sealed interface MutablePlayerKeyedSignal<T> extends PlayerKeyedSignal<T>, MutableKeyedSignal<UUID, T> permits MutablePlayerKeyedSignalImpl {

    /**
     * 返回指定玩家分区的可写稳定句柄.
     *
     * @param player 玩家
     * @return 可写的玩家分区句柄
     */
    @Override
    @NotNull
    default MutableSignal<T> at(@NotNull Player player) {
        return this.at(player.uuid());
    }

    /**
     * 写入指定玩家分区的新值.
     *
     * @param player 玩家
     * @param value 新值, 允许为 {@code null}
     */
    default void set(@NotNull Player player, T value) {
        this.set(player.uuid(), value);
    }

    /**
     * 基于指定玩家分区的当前值原子更新. 发生争用时 {@code updater} 可能执行多次.
     * <p><strong>{@code updater} 必须无副作用并允许重试</strong>.
     *
     * @param player 玩家
     * @param updater 根据当前值计算新值的纯函数
     */
    default void update(@NotNull Player player, @NotNull UnaryOperator<T> updater) {
        this.update(player.uuid(), updater);
    }
}

