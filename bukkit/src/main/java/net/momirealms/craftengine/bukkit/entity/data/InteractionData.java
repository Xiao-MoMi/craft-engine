package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

public class InteractionData<T> extends BaseEntityData<T> {
    public static final InteractionData<Float> Width = new InteractionData<>(InteractionData.class, EntityDataSerializersProxy.FLOAT, 1F);
    public static final InteractionData<Float> Height = new InteractionData<>(InteractionData.class, EntityDataSerializersProxy.FLOAT, 1F);
    public static final InteractionData<Boolean> Response = new InteractionData<>(InteractionData.class, EntityDataSerializersProxy.BOOLEAN, false);

    protected InteractionData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
