package net.momirealms.craftengine.bukkit.entity.data.decoration;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

public class HangingEntityData<T> extends BlockAttachedEntityData<T> {
    // 1.21.6+
    public static final HangingEntityData<Object> Direction = of(HangingEntityData.class, EntityDataSerializersProxy.DIRECTION, DirectionProxy.SOUTH, VersionHelper.isOrAbove1_21_6);

    private static <T> HangingEntityData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new HangingEntityData<>(clazz, serializer, defaultValue);
    }

    protected HangingEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
