package net.momirealms.craftengine.core.plugin.ui.window;

import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 表示一名玩家正在查看的 UI Window.
 */
public interface Window {

    /**
     * 返回查看这扇 Window 的玩家.
     *
     * @return 查看者
     */
    @NotNull
    Player viewer();

    /**
     * 读取指定槽位最近一次渲染记录的交互数据.
     *
     * @param windowSlot Window 槽位
     * @return 记录的数据, 槽位没有记录时为 {@code null}
     */
    @Nullable
    Object rememberedAt(int windowSlot);
}
