package net.momirealms.craftengine.core.entity.furniture;

import org.joml.Vector3f;

public abstract class AbstractFurnitureEmitter implements FurnitureEmitter {
    protected final Vector3f position;
    protected boolean emitting = false;

    protected AbstractFurnitureEmitter(Vector3f position) {
        this.position = position;
    }

    @Override
    public Vector3f position() {
        return position;
    }

    @Override
    public boolean isEmitting() {
        return emitting;
    }

    protected void setEmitting(boolean emitting) {
        this.emitting = emitting;
    }
}
