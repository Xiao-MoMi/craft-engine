package net.momirealms.craftengine.core.plugin.ui.item;

public interface ObservableItem extends Item {

    /**
     * 通知所有当前挂载槽位重新渲染此 Item.
     */
    void notifyWindows();
}
