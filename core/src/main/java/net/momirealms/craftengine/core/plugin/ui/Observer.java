package net.momirealms.craftengine.core.plugin.ui;

@FunctionalInterface
public interface Observer<T> {

    void onUpdate(T update);
}
