package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.entity.data.ClassTreeIdRegistry;
import net.momirealms.craftengine.core.entity.data.EntityData;

public class BukkitEntityData<T> implements EntityData<T> {
    public static final ClassTreeIdRegistry ID_REGISTRY = new ClassTreeIdRegistry();
    private final int id;
    private final Object serializer;
    private final T defaultValue;
    private final Object entityDataAccessor;

    public BukkitEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        this.id = ID_REGISTRY.define(clazz);
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

    @Override
    public Object create(Object entityDataAccessor, Object value) {
        return EntityDataValue.create(entityDataAccessor, value);
    }
}
