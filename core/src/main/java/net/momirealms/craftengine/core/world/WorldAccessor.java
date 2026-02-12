package net.momirealms.craftengine.core.world;

public interface WorldAccessor extends BlockAccessor, WorldHeightAccessor {

    Object serverWorld();

    default Object literalObject() {
        return serverWorld();
    }
}
