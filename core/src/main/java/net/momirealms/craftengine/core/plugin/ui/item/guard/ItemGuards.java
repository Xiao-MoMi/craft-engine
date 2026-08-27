package net.momirealms.craftengine.core.plugin.ui.item.guard;

import net.momirealms.craftengine.core.entity.player.GameMode;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class ItemGuards {

    private ItemGuards() {
    }

    /**
     * 创建要求玩家拥有指定权限的守卫.
     *
     * @param <C> 交互类型
     * @param permission 权限节点
     * @return 权限守卫
     */
    @NotNull
    public static <C extends ItemInteraction> ItemGuard<C> permission(@NotNull String permission) {
        return (ignoredItem, interaction) -> interaction.player().hasPermission(permission);
    }

    /**
     * 创建要求玩家处于指定游戏模式的守卫.
     *
     * @param <C> 交互类型
     * @param gameMode 游戏模式
     * @return 游戏模式守卫
     */
    @NotNull
    public static <C extends ItemInteraction> ItemGuard<C> gameMode(@NotNull GameMode gameMode) {
        return (ignoredItem, interaction) -> interaction.player().gameMode() == gameMode;
    }

    /**
     * 创建按 Item 与玩家分别计时的点击节流守卫.
     *
     * @param intervalMillis 两次有效点击之间至少间隔的毫秒数
     * @return 节流守卫
     * @throws IllegalArgumentException 当间隔不是正数时
     */
    @NotNull
    public static ItemGuard<ItemClick> throttle(long intervalMillis) {
        return new ThrottleGuard(TimeUnit.MILLISECONDS.toNanos(intervalMillis), System::nanoTime);
    }
}
