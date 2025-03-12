package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.Key;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FurnitureElement {
    private final Key item;
    private final Billboard billboard;
    private final ItemDisplayContext transform;
    private final Vector3f scale;
    private final Vector3f translation;
    private final Vector3f offset;
    private final Quaternionf rotation;

    public FurnitureElement(Key item, Billboard billboard, ItemDisplayContext transform, Vector3f scale, Vector3f translation, Vector3f offset, Quaternionf rotation) {
        this.billboard = billboard;
        this.transform = transform;
        this.scale = scale;
        this.translation = translation;
        this.item = item;
        this.rotation = rotation;
        this.offset = offset;
    }

    public Quaternionf rotation() {
        return rotation;
    }

    public Key item() {
        return item;
    }

    public Billboard billboard() {
        return billboard;
    }

    public ItemDisplayContext transform() {
        return transform;
    }

    public Vector3f scale() {
        return scale;
    }

    public Vector3f translation() {
        return translation;
    }

    public Vector3f offset() {
        return offset;
    }
}
