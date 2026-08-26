package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.ui.interaction.UiClickType;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

/**
 * 一次 UI Item 点击.
 *
 * @param clickType 点击类型
 * @param player 点击玩家
 * @param window 所属 Window
 * @param cursor 派发时的光标快照, 构造时复制
 * @param windowSlot 点击的 Window 槽位
 * @param hotbarButton 数字键对应的快捷栏索引, 未关联时为 {@code -1}
 */
public record ItemClick(
        @NotNull UiClickType clickType,
        @NotNull Player player,
        @NotNull Window window,
        @NotNull Item cursor,
        int windowSlot,
        int hotbarButton
) implements ItemInteraction {

    public ItemClick {
        cursor = cursor.copy();
    }

    public ItemClick(@NotNull Player player, @NotNull UiClickType clickType, @NotNull Window window, @NotNull Item cursor, int windowSlot) {
        this(clickType, player, window, cursor, windowSlot, -1);
    }
}
