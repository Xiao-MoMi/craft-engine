package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.Billboard;
import net.momirealms.craftengine.core.entity.ItemDisplayContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    void initPackets(int entityId, @NotNull WorldPosition position, @NotNull Quaternionf conjugated, @Nullable Integer dyedColor, Consumer<Object> packets);
}
