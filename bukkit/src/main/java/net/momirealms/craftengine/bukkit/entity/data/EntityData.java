package net.momirealms.craftengine.bukkit.entity.data;

import java.util.List;

public interface EntityData<T> {

    Object serializer();
    int id();
    T defaultValue();
    Object entityDataAccessor();

    default Object createEntityDataIfNotDefaultValue(T value) {
        if (defaultValue().equals(value)) return null;
        return EntityDataValue.create(id(), serializer(), entityDataAccessor(), value);
    }

    default void addEntityDataIfNotDefaultValue(T value, List<Object> list) {
        if (!defaultValue().equals(value)) {
            list.add(EntityDataValue.create(id(), serializer(), entityDataAccessor(), value));
        }
    }

    default void addEntityData(T value, List<Object> list) {
        list.add(EntityDataValue.create(id(), serializer(), entityDataAccessor(), value));
    }

    static <T> EntityData<T> of(int id, Object serializer, T defaultValue) {
        return new SimpleEntityData<>(id, serializer, defaultValue);
    }
}
