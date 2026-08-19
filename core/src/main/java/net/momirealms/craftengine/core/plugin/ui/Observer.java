package net.momirealms.craftengine.core.plugin.ui;

@FunctionalInterface
public interface Observer<T> {

    /**
     * 接收来自 {@link Observable} 的类型化更新.
     *
     * @param update 更新类型
     */
    void onUpdate(T update);
}
