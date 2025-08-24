package net.momirealms.craftengine.bukkit.entity.data;

public class BlockAttachedEntityData<T> extends BaseEntityData<T> {

    public BlockAttachedEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
