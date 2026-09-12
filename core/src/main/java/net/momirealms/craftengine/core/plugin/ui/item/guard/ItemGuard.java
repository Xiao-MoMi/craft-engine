package net.momirealms.craftengine.core.plugin.ui.item.guard;

import net.momirealms.craftengine.core.plugin.ui.item.Item;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ItemGuard<C extends ItemInteraction> {
    /** 永远允许交互的共享守卫. */
    ItemGuard<ItemInteraction> ALLOW_ALL = (ignoredItem, ignoredInteraction) -> true;

    /**
     * 判断 Item 是否接受本次交互.
     *
     * @param item 接收交互的 Item
     * @param interaction 交互上下文
     * @return 接受交互时返回 {@code true}
     */
    boolean test(@NotNull Item item, @NotNull C interaction);

    /**
     * 把下一个守卫接到链尾, 整条链在首个 {@code false} 处结束.
     *
     * @param guard 下一个守卫
     * @return 组合后的守卫
     */
    @NotNull
    default ItemGuard<C> and(@NotNull ItemGuard<C> guard) {
        return (item, interaction) -> this.test(item, interaction) && guard.test(item, interaction);
    }

    /**
     * 把带拒绝回调的守卫接到链尾, 回调只在新接入的守卫返回 {@code false} 时执行.
     *
     * @param guard 下一个守卫
     * @param onRejected 下一个守卫的拒绝回调
     * @return 组合后的守卫
     */
    @NotNull
    default ItemGuard<C> and(@NotNull ItemGuard<C> guard, @NotNull BiConsumer<Item, C> onRejected) {
        return (item, interaction) -> {
            if (!this.test(item, interaction)) return false;
            if (guard.test(item, interaction)) return true;
            onRejected.accept(item, interaction);
            return false;
        };
    }

    /**
     * 为当前守卫附加拒绝回调.
     *
     * @param onRejected 当前守卫的拒绝回调
     * @return 带拒绝回调的守卫
     */
    @NotNull
    default ItemGuard<C> onRejected(@NotNull BiConsumer<Item, C> onRejected) {
        return (item, interaction) -> {
            if (this.test(item, interaction)) return true;
            onRejected.accept(item, interaction);
            return false;
        };
    }
}
