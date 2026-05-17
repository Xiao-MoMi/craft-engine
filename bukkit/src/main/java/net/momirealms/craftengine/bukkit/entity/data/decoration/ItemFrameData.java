package net.momirealms.craftengine.bukkit.entity.data.decoration;

import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

public class ItemFrameData<T> extends HangingEntityData<T> {
    public static final ItemFrameData<Object> Item = new ItemFrameData<>(ItemFrameData.class, EntityDataSerializersProxy.ITEM_STACK, ItemStackProxy.EMPTY);
    public static final ItemFrameData<Integer> Rotation = new ItemFrameData<>(ItemFrameData.class, EntityDataSerializersProxy.INT, 0);

    protected ItemFrameData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
