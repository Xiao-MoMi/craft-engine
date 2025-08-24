package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;

public class BlockDisplayEntityData<T> extends DisplayEntityData<T> {
    // Block display only
    public static final BlockDisplayEntityData<Object> DisplayedBlock = new BlockDisplayEntityData<>(BlockDisplayEntityData.class, EntityDataValue.Serializers$BLOCK_STATE, MBlocks.AIR$defaultState);

    public BlockDisplayEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
