package net.momirealms.craftengine.core.plugin.ui;

public interface Subscription extends AutoCloseable {

    boolean isClosed();

    @Override
    void close();
}
