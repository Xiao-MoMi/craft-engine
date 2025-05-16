package net.momirealms.craftengine.core.entity;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.UUID;

public interface Entity {
    Key type();

    double x();

    double y();

    double z();

    WorldPosition position();

    void tick();

    float getXRot();

    int entityID();

    float getYRot();

    World world();

    Direction getDirection();

    Object literalObject();

    String name();

    UUID uuid();
}
