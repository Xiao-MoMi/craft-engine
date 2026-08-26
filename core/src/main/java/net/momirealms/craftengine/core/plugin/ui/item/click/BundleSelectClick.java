package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

/**
 * 一次 Bundle 内容槽位选择.
 *
 * @param player 执行选择的玩家
 * @param window 所属 Window
 * @param windowSlot Bundle 所在的 Window 槽位
 * @param bundleSlot Bundle 内槽位, {@code -1} 表示没有选中槽位
 */
public record BundleSelectClick(@NotNull Player player, @NotNull Window window, int windowSlot, int bundleSlot) implements ItemInteraction {
}