package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;

public class ItemFrameData<T> extends HangingEntityData<T> {
    public static final ItemFrameData<Object> Item = new ItemFrameData<>(ItemFrameData.class, EntityDataValue.Serializers$ITEM_STACK, CoreReflections.instance$ItemStack$EMPTY);
    public static final ItemFrameData<Integer> Rotation = new ItemFrameData<>(ItemFrameData.class, EntityDataValue.Serializers$INT, 0);

    public ItemFrameData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
