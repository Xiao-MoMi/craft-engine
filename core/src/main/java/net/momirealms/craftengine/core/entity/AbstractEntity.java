package net.momirealms.craftengine.core.entity;

import net.momirealms.craftengine.core.world.WorldPosition;

public abstract class AbstractEntity implements Entity {

    @Override
    public WorldPosition position() {
        return new WorldPosition(world(), x(), y(), z(), getXRot(), getYRot());
    }
}
