package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.nms.FastNMS;

public class SimpleEntityData<T> implements EntityData<T> {
    private final Object entityDataAccessor;
    private final T defaultValue;

    public SimpleEntityData(int id, Object serializer, T defaultValue) {
        this.entityDataAccessor = FastNMS.INSTANCE.constructor$EntityDataAccessor(id, serializer);
        this.defaultValue = defaultValue;
    }

    @Override
    public T defaultValue() {
        return this.defaultValue;
    }

    @Override
    public Object entityDataAccessor() {
        return this.entityDataAccessor;
    }
}
