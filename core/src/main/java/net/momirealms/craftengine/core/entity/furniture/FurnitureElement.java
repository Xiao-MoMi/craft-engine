package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.Billboard;
import net.momirealms.craftengine.core.entity.ItemDisplayContext;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

public interface FurnitureElement {
    Quaternionf rotation();

    Key item();

    Billboard billboard();

    ItemDisplayContext transform();

    boolean applyDyedColor();

    Vector3f scale();

    Vector3f translation();

    Vector3f position();

    void initPackets(Furniture furniture, int entityId, @NotNull Quaternionf conjugated, Consumer<Object> packets);

    interface Builder {

        Builder item(Key item);

        Builder billboard(Billboard billboard);

        Builder transform(ItemDisplayContext transform);

        Builder scale(Vector3f scale);

        Builder translation(Vector3f translation);

        Builder position(Vector3f position);

        Builder rotation(Quaternionf rotation);

        Builder applyDyedColor(boolean applyDyedColor);

        FurnitureElement build();
    }
}
