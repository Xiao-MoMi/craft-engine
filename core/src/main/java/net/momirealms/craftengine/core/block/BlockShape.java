package net.momirealms.craftengine.core.block;

public interface BlockShape {

    Object getShape(Object thisObj, Object[] args);

    Object getCollisionShape(Object thisObj, Object[] args);

    Object getSupportShape(Object thisObj, Object[] args);
}
