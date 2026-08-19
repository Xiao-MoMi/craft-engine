package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

/**
 * 一次物品点击的上下文.
 *
 * @param clickType 点击类型
 * @param player 点击玩家
 * @param window 当前 Window
 * @param windowSlot 点击的 Window 槽位
 * @param hotbarButton ClickType 是 NUMBER_KEY 对应的快捷栏索引, 未关联快捷栏时为 {@code -1}
 * @param cursor 派发时菜单持有的实际光标快照
 */
public record ItemClick (
        @NotNull ClickType clickType,
        @NotNull Player player,
        @NotNull Window window,
        @NotNull Item cursor,
        int windowSlot,
        int hotbarButton
) implements ItemInteraction {

    public ItemClick {
        cursor = cursor.clone();
    }

    public ItemClick(@NotNull Player player, @NotNull ClickType clickType, @NotNull Window window, @NotNull Item cursor, int windowSlot) {
        this(clickType, player, window, cursor, windowSlot, -1);
    }
}
