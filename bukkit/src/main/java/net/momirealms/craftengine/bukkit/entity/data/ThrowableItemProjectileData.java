package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.util.Reflections;

public class ThrowableItemProjectileData<T> extends BaseEntityData<T> {
    public static final ThrowableItemProjectileData<Object> ItemStack = new ThrowableItemProjectileData<>(8, EntityDataValue.Serializers$ITEM_STACK, Reflections.instance$ItemStack$Air);

    public ThrowableItemProjectileData(int id, Object serializer, T defaultValue) {
        super(id, serializer, defaultValue);
    }
}
