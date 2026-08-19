package net.momirealms.craftengine.core.plugin.ui.item;

public interface ObservableItem extends Item {

    // 通知所有当前挂载此 Item 的最终 Window 槽位重新渲染.
    void notifyWindows();
}
