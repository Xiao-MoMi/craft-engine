package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;

public class ItemDisplayEntityData<T> extends DisplayEntityData<T> {
    // Item display only
    public static final ItemDisplayEntityData<Object> DisplayedItem = new ItemDisplayEntityData<>(ItemDisplayEntityData.class, EntityDataValue.Serializers$ITEM_STACK, CoreReflections.instance$ItemStack$EMPTY);
    /**
     * Display type:
     * 0 = NONE
     * 1 = THIRD_PERSON_LEFT_HAND
     * 2 = THIRD_PERSON_RIGHT_HAND
     * 3 = FIRST_PERSON_LEFT_HAND
     * 4 = FIRST_PERSON_RIGHT_HAND
     * 5 = HEAD
     * 6 = GUI
     * 7 = GROUND
     * 8 = FIXED
     */
    public static final ItemDisplayEntityData<Byte> DisplayType = new ItemDisplayEntityData<>(ItemDisplayEntityData.class, EntityDataValue.Serializers$BYTE, (byte) 0);

    public ItemDisplayEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
