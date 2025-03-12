package net.momirealms.craftengine.core.plugin;

public interface Reloadable {

    default void reload() {
        unload();
        load();
    }

    default void enable() {
    }

    default void unload() {
    }

    default void load() {
    }

    default void disable() {
        unload();
    }
}
