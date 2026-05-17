package net.momirealms.craftengine.bukkit.entity.data.item;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

public class ItemEntityData<T> extends BaseEntityData<T> {
    public static final ItemEntityData<Object> Item = new ItemEntityData<>(ItemEntityData.class, EntityDataSerializersProxy.ITEM_STACK, ItemStackProxy.EMPTY);

    protected ItemEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
