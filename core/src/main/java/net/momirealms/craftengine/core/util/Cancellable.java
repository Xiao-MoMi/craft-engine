package net.momirealms.craftengine.core.util;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancel);
}
