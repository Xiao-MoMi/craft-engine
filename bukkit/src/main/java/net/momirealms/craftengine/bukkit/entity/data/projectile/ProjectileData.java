package net.momirealms.craftengine.bukkit.entity.data.projectile;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;

public class ProjectileData<T> extends BaseEntityData<T> {

    protected ProjectileData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
