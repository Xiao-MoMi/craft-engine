package net.momirealms.craftengine.core.entity;

public interface CollisionEntity extends NonTickingEntity, UntrackedEntity {

    void destroy();

    int getEntityId();
}
