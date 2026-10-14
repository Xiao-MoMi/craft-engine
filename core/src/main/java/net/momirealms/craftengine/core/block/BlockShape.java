package net.momirealms.craftengine.core.block;

public interface BlockShape {

    Object getShape(Object thisObj, Object blockState, Object level, Object blockPos, Object collisionContext);

    Object getCollisionShape(Object thisObj, Object blockState, Object level, Object blockPos, Object collisionContext);

    Object getSupportShape(Object thisObj, Object blockState, Object level, Object blockPos);
}
