package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.ui.interaction.UIClickType;
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
public record ItemDrag(@NotNull UIClickType clickType,
                       @NotNull Player player,
                       @NotNull Window window,
                       @NotNull Item cursor,
                       int windowSlot,
                       @NotNull List<Stop> path) implements ItemInteraction {

    public ItemDrag {
        cursor = cursor.copy();
        path = List.copyOf(path);
    }

    /**
     * 返回本次 Drag 经过的全部节点中的本次对应的节点.
     *
     * @return 当前站点
     * @throws IllegalStateException 当 path 不包含当前 Window 槽位时
     */
    @NotNull
    public Stop currentStop() {
        return this.path.get(this.index());
    }

    /**
     * 返回当前槽位在拖拽路径中的索引.
     *
     * @return 从 0 开始的路径索引
     * @throws IllegalStateException 当 path 不包含当前 Window 槽位时
     */
    public int index() {
        for (int index = 0; index < this.path.size(); index++) {
            if (this.path.get(index).windowSlot() == this.windowSlot) return index;
        }
        throw new IllegalStateException("drag path does not contain window slot " + this.windowSlot);
    }

    /**
     * 拖拽路径经过的一个 Window 槽位.
     *
     * @param windowSlot Window 槽位
     */
    public record Stop(int windowSlot) {
    }
}
