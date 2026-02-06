package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

public class ItemEntityData<T> extends BaseEntityData<T> {
    public static final ItemEntityData<Object> Item = new ItemEntityData<>(ItemEntityData.class, EntityDataValue.Serializers$ITEM_STACK, ItemStackProxy.EMPTY);

    public ItemEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
