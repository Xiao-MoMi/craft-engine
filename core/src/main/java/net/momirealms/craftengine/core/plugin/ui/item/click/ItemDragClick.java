package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.ui.interaction.UiClickType;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 一次拖拽手势经过某个 UI Item 时的上下文, cursor 与 path 在构造时复制.
 *
 * @param clickType 拖拽使用的按键
 * @param player 拖拽玩家
 * @param window 所属 Window
 * @param cursor 手势开始时的光标快照
 * @param windowSlot 本次派发对应的 Window 槽位
 * @param path 手势经过的全部 Window 槽位
 */
public record ItemDragClick(
        @NotNull UiClickType clickType,
        @NotNull Player player,
        @NotNull Window window,
        @NotNull Item cursor,
        int windowSlot,
        @NotNull List<Stop> path
) implements ItemInteraction {

    public ItemDragClick {
        cursor = cursor.copy();
        path = List.copyOf(path);
    }

    /**
     * 返回本次派发对应的路径站点.
     *
     * @return 当前站点
     * @throws IllegalStateException 当 path 不包含当前 Window 槽位时
     */
    @NotNull
    public Stop self() {
        return this.path.get(this.stopIndex());
    }

    /**
     * 返回当前槽位在拖拽路径中的索引.
     *
     * @return 从 0 开始的路径索引
     * @throws IllegalStateException 当 path 不包含当前 Window 槽位时
     */
    public int stopIndex() {
        for (int index = 0; index < this.path.size(); index++) {
            if (this.path.get(index).windowSlot() == this.windowSlot) return index;
        }
        throw new IllegalStateException("drag path does not contain window slot " + this.windowSlot);
    }

    /**
     * 拖拽路径经过的一个 Window 槽位.
     *
     * @param windowSlot Window 槽位
     * @param kind 显示路径终点类型
     */
    public record Stop(int windowSlot, @NotNull Kind kind) {
    }

    /**
     * 显示路径的终点类型.
     */
    public enum Kind {
        /** 终点是 Item, 会收到拖拽交互. */
        ITEM,
        /** 终点连接 Inventory. */
        INVENTORY,
        /** 终点是没有 Item 或 Inventory 的空槽. */
        EMPTY,
        /** 路径经过冻结 Pane, 不参与交互. */
        FROZEN
    }
}
