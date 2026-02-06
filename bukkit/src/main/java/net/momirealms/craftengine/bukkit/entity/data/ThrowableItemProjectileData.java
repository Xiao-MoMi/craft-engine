package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

public class ThrowableItemProjectileData<T> extends BaseEntityData<T> {
    public static final ThrowableItemProjectileData<Object> ItemStack = new ThrowableItemProjectileData<>(ThrowableItemProjectileData.class, EntityDataValue.Serializers$ITEM_STACK, ItemStackProxy.EMPTY);

    public ThrowableItemProjectileData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
