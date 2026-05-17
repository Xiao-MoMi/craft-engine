package net.momirealms.craftengine.core.world;

public interface WorldAccessor extends BlockAccessor, WorldHeightAccessor {

    Object minecraftWorld();

    default Object generatingWorld() {
        return null;
    }
}
