package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.Key;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface HitBox {

    Key type();

    void addSpawnPackets(int[] entityId, double x, double y, double z, float yaw, Quaternionf conjugated, Consumer<Object> packets);

    int[] acquireEntityIds(Supplier<Integer> entityIdSupplier);

    Seat[] seats();

    Vector3f position();
}
