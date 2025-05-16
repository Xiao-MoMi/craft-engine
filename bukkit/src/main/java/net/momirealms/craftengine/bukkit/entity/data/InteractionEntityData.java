package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.core.util.VersionHelper;

public class InteractionEntityData<T> extends BaseEntityData<T> {
    public static final InteractionEntityData<Float> Width = of(8, EntityDataValue.Serializers$FLOAT, 1F);
    public static final InteractionEntityData<Float> Height = of(9, EntityDataValue.Serializers$FLOAT, 1F);
    public static final InteractionEntityData<Boolean> Responsive = of(10, EntityDataValue.Serializers$BOOLEAN, false);

    public static <T> InteractionEntityData<T> of(final int id, final Object serializer, T defaultValue) {
        return new InteractionEntityData<>(id, serializer, defaultValue);
    }

    public InteractionEntityData(int id, Object serializer, T defaultValue) {
        super(!VersionHelper.isOrAbove1_20_2() && id >= 11 ? id - 1 : id, serializer, defaultValue);
    }
}
