package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.core.entity.data.ClassTreeIdRegistry;
import net.momirealms.craftengine.core.entity.data.EntityData;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

public class BukkitEntityData<T> implements EntityData<T> {
    public static final ClassTreeIdRegistry ID_REGISTRY = new ClassTreeIdRegistry();
    public final int id;
    public final Object serializer;
    public final T defaultValue;
    public final Object entityDataAccessor;

    protected BukkitEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        this.id = ID_REGISTRY.define(clazz);
        this.serializer = serializer;
        this.defaultValue = defaultValue;
        this.entityDataAccessor = EntityDataAccessorProxy.INSTANCE.newInstance(id, serializer);
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
    public Object create(Object entityDataAccessor, T value) {
        return SynchedEntityDataProxy.DataValueProxy.INSTANCE.create(entityDataAccessor, value);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id=" + id +
                ", serializer=" + serializer +
                ", defaultValue=" + defaultValue +
                ", entityDataAccessor=" + entityDataAccessor +
                '}';
    }
}
