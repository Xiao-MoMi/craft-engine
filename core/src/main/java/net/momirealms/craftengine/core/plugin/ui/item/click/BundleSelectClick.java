package net.momirealms.craftengine.core.plugin.ui.item.click;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家在 Item 显示的 Bundle 内容中选择槽位的上下文.
 *
 * @param player 执行选择的玩家
 * @param window 当前 Window
 * @param windowSlot Bundle 所在的 Window 槽位
 * @param bundleSlot Bundle 内槽位; {@code -1} 表示光标已离开.
 */
public record BundleSelectClick(
        @NotNull Player player,
        @NotNull Window window,
        int windowSlot,
        int bundleSlot
) implements ItemInteraction {

    public BundleSelectClick {
        if (bundleSlot < -1) {
            throw new IllegalArgumentException("bundleSlot must be -1 or non-negative");
        }
    }
}
