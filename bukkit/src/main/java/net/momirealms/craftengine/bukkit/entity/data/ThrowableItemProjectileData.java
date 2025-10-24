package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;

public class ThrowableItemProjectileData<T> extends BaseEntityData<T> {
    public static final ThrowableItemProjectileData<Object> ItemStack = new ThrowableItemProjectileData<>(ThrowableItemProjectileData.class, EntityDataValue.Serializers$ITEM_STACK, CoreReflections.Instance.itemStack$EMPTY);

    public ThrowableItemProjectileData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
