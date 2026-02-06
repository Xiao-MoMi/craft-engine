package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.proxy.minecraft.world.entity.PoseProxy;

import java.util.Optional;

public class BaseEntityData<T> extends BukkitEntityData<T> {
    public static final BaseEntityData<Byte> SharedFlags = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$BYTE, (byte) 0);
    public static final BaseEntityData<Integer> AirSupply = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$INT, 300);
    public static final BaseEntityData<Optional<Object>> CustomName = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$OPTIONAL_COMPONENT, Optional.empty());
    public static final BaseEntityData<Boolean> CustomNameVisible = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$BOOLEAN, false);
    public static final BaseEntityData<Boolean> Silent = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$BOOLEAN, false);
    public static final BaseEntityData<Boolean> NoGravity = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$BOOLEAN, false);
    public static final BaseEntityData<Object> Pose = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$POSE, PoseProxy.STANDING);
    public static final BaseEntityData<Integer> TicksFrozen = new BaseEntityData<>(BaseEntityData.class, EntityDataValue.Serializers$INT, 0);

    public BaseEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
