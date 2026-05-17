package net.momirealms.craftengine.bukkit.entity.data.decoration;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;

public class BlockAttachedEntityData<T> extends BaseEntityData<T> {

    protected BlockAttachedEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
