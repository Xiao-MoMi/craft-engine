package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.nms.FastNMS;

public class SimpleEntityData<T> implements EntityData<T> {
    private final int id;
    private final Object serializer;
    private final T defaultValue;
    private final Object entityDataAccessor;

    public SimpleEntityData(int id, Object serializer, T defaultValue) {
        this.id = id;
        this.serializer = serializer;
        this.defaultValue = defaultValue;
        this.entityDataAccessor = FastNMS.INSTANCE.constructor$EntityDataAccessor(id, serializer);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Object serializer() {
        return serializer;
    }

    @Override
    public T defaultValue() {
        return defaultValue;
    }

    @Override
    public Object entityDataAccessor() {
        return entityDataAccessor;
    }
}
