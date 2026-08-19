package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 一次拖拽手势经过某个 Item 时的上下文.
 * <p>只有手势本身成立才会派发, 光标为空, 或非创造模式的中键拖拽, 都不会到达 Item.
 * <p>同一次手势给经过的每一个 Item 槽位各派发一次; 同一个 Item 实例挂在多个被拖过的槽位上时会被调用多次, 每次的 {@link #windowSlot()} 不同, {@link #path()} 是同一份.
 * <p>{@code path} 是手势发生那一刻的结构快照. 处理器在第一站改动 Pane 结构不会让后续站点的 {@link Stop#kind()} 跟着变.
 *
 * @param clickType 拖拽按键: LEFT 均分, RIGHT 每格一个, MIDDLE 创造模式整堆
 * @param player 拖拽玩家
 * @param window 当前 Window
 * @param cursor 手势开始时的光标快照, 即引擎分配之前的整堆
 * @param windowSlot 本次派发对应的 Window 槽位
 * @param path 手势经过的全部 Window 槽位, 按客户端发包顺序保序去重
 */
public record ItemDragClick(
        @NotNull ClickType clickType,
        @NotNull Player player,
        @NotNull Window window,
        @NotNull Item cursor,
        int windowSlot,
        @NotNull List<Stop> path
) implements ItemInteraction {

    public ItemDragClick {
        cursor = cursor.clone();
        path = List.copyOf(path);
    }

    @NotNull
    public Stop self() {
        return this.path.get(this.stopIndex());
    }

    public int stopIndex() {
        for (int index = 0; index < this.path.size(); index++) {
            if (this.path.get(index).windowSlot() == this.windowSlot) return index;
        }
        throw new IllegalStateException("drag path does not contain window slot " + this.windowSlot);
    }

    @NotNull
    public Item displayed() {
        return this.window.displayedAt(this.windowSlot);
    }

    @NotNull
    public Item displayedAt(int windowSlot) {
        return this.window.displayedAt(windowSlot);
    }

    public record Stop(int windowSlot, @NotNull Kind kind) {
    }

    public enum Kind {
        ITEM,       // 终点是 Item, 会收到 ItemDragClick
        INVENTORY,  // 终点连着 Inventory, 是否真的收到物品由放入规则和引擎决定
        EMPTY,      // 终点是空槽, 只有背景
        FROZEN      // 路径经过已冻结 Pane, 不参与任何交互
    }
}
