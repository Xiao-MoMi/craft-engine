package net.momirealms.craftengine.bukkit.entity.data.projectile;

import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

public class ThrowableItemProjectileData<T> extends ThrowableProjectileData<T> {
    public static final ThrowableItemProjectileData<Object> Item = new ThrowableItemProjectileData<>(ThrowableItemProjectileData.class, EntityDataSerializersProxy.ITEM_STACK, ItemStackProxy.EMPTY);

    protected ThrowableItemProjectileData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
