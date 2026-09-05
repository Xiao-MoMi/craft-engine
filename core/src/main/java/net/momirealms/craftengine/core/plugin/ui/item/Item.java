package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemDrag;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

public interface Item {
    Item EMPTY = new EmptyItem();

    @NotNull
    static Item empty() {
        return EMPTY;
    }

    @NotNull
    static Item simple(@NotNull net.momirealms.craftengine.core.item.Item template) {
        return new StaticItem(ItemProvider.constant(template));
    }

    @NotNull
    static Item simple(@NotNull ItemProvider itemProvider) {
        return new StaticItem(itemProvider);
    }

    @NotNull
    static ItemBuilder builder() {
        return new ItemBuilder();
    }

    /**
     * 返回此 Item 的显示来源.
     *
     * @return 显示来源
     */
    @NotNull
    ItemProvider getItemProvider();

    /**
     * 返回首次成功渲染前使用的占位来源.
     *
     * @return 占位来源
     */
    @NotNull
    default ImmediateItemProvider getPlaceholder() {
        return ItemProvider.EMPTY;
    }

    /**
     * 处理一次 Item 点击.
     *
     * @param click 点击上下文
     */
    default void handleClick(@NotNull ItemClick click) {
    }

    /**
     * 处理一次经过此 Item 的拖拽.
     *
     * @param drag 拖拽上下文
     */
    default void handleDrag(@NotNull ItemDrag drag) {
    }

    /**
     * 处理一次 Bundle 内容槽位选择.
     *
     * @param select 选择上下文
     */
    default void handleBundleSelect(@NotNull BundleSelectClick select) {
    }

    /**
     * 将 Item 挂载到一个最终显示槽位, 返回的 attachment 必须随显示路径关闭.
     *
     * @param window 所属 Window
     * @param observer Item 失效观察者
     * @return 本次显示关系
     */
    default ItemAttachment attach(@NotNull Window window, @NotNull Observer<? super Item> observer) {
        return ItemAttachment.PASSIVE;
    }
}
