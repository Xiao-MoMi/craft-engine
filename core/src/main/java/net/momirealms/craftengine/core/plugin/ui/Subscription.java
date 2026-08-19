package net.momirealms.craftengine.core.plugin.ui;

/**
 * 持有 {@link Observable} 与 {@link Observer} 之间的一条订阅关系.
 */
public interface Subscription extends AutoCloseable {

    boolean isClosed();

    @Override
    void close();
}
