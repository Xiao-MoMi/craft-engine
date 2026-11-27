package net.momirealms.craftengine.core.entity.furniture.tick;

public interface TickingFurniture {

    void tick();

    boolean isValid();

    int entityId();
}
