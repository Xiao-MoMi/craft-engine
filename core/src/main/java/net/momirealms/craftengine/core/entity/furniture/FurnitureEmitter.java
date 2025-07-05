package net.momirealms.craftengine.core.entity.furniture;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.momirealms.craftengine.core.util.Key;

public interface FurnitureEmitter {

    Key type();

    Vector3f position();

    void startEmission(Furniture furniture, @NotNull Quaternionf conjugated);

    void stopEmission(Furniture furniture);

    boolean isEmitting();

    interface Builder {

        Builder position(Vector3f position);

        FurnitureEmitter build();
    }
}
