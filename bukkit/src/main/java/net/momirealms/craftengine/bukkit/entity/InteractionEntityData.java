package net.momirealms.craftengine.bukkit.entity;

import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.List;

public class InteractionEntityData<T> {

    private final int id;
    private final Object serializer;
    private final T defaultValue;

    // Entity
    public static final InteractionEntityData<Byte> EntityMasks = of(0, EntityDataValue.Serializers$BYTE, (byte) 0);
    // Interaction only
    public static final InteractionEntityData<Float> Width = of(8, EntityDataValue.Serializers$FLOAT, 1F);
    public static final InteractionEntityData<Float> Height = of(9, EntityDataValue.Serializers$FLOAT, 1F);
    public static final InteractionEntityData<Boolean> Responsive = of(10, EntityDataValue.Serializers$BOOLEAN, false);

    public static <T> InteractionEntityData<T> of(final int id, final Object serializer, T defaultValue) {
        return new InteractionEntityData<>(id, serializer, defaultValue);
    }

    public InteractionEntityData(int id, Object serializer, T defaultValue) {
        if (!VersionHelper.isVersionNewerThan1_20_2()) {
            if (id >= 11) {
                id--;
            }
        }
        this.id = id;
        this.serializer = serializer;
        this.defaultValue = defaultValue;
    }

    public Object serializer() {
        return serializer;
    }

    public int id() {
        return id;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public Object createEntityDataIfNotDefaultValue(T value) {
        if (defaultValue().equals(value)) return null;
        return EntityDataValue.create(id, serializer, value);
    }

    public void addEntityDataIfNotDefaultValue(T value, List<Object> list) {
        if (defaultValue().equals(value)) return;
        list.add(EntityDataValue.create(id, serializer, value));
    }

    public void addEntityData(T value, List<Object> list) {
        list.add(EntityDataValue.create(id, serializer, value));
    }
}
