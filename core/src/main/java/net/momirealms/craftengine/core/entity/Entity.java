package net.momirealms.craftengine.core.entity;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.World;

public abstract class Entity {

    public abstract double x();

    public abstract double y();

    public abstract double z();

    public abstract void tick();

    public abstract int entityID();

    public abstract float getXRot();

    public abstract float getYRot();

    public abstract World level();

    public abstract Direction getDirection();
}
