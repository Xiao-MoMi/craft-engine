package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;

public class HangingEntityData<T> extends BlockAttachedEntityData<T> {
    // 1.21.6+
    public static final HangingEntityData<Object> Direction = of(HangingEntityData.class, EntityDataValue.Serializers$DIRECTION, DirectionProxy.SOUTH, VersionHelper.isOrAbove1_21_6());

    public static <T> HangingEntityData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new HangingEntityData<>(clazz, serializer, defaultValue);
    }

    public HangingEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
