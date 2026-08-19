package net.momirealms.craftengine.core.plugin.ui.item.guard;

import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ItemGuards {

    private ItemGuards() {
    }

    @NotNull
    public static ItemGuard<ItemInteraction> permission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return (ignoredItem, interaction) -> interaction.player().hasPermission(permission);
    }

    /**
     * 创建按 Item 与玩家分别计时的节流规则.
     * <p>首次点击立即通过, 限制期内的拒绝不会延长间隔.</p>
     *
     * @param intervalMillis 两次有效点击之间至少间隔的毫秒数
     */
    @NotNull
    public static ItemGuard<ItemClick> throttle(long intervalMillis) {
        return new ThrottleGuard(intervalMillis, System::currentTimeMillis);
    }
}
