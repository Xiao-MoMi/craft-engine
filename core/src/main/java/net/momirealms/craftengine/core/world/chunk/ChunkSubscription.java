package net.momirealms.craftengine.core.world.chunk;

public interface ChunkSubscription {

    void cancel();

    boolean cancelled();
}
