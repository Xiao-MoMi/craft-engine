package net.momirealms.craftengine.bukkit.entity.data.animal.happyghast;

import net.momirealms.craftengine.bukkit.entity.data.animal.AnimalData;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

public class HappyGhastData<T> extends AnimalData<T> {
    public static final HappyGhastData<Boolean> IsLeashHolder = new HappyGhastData<>(HappyGhastData.class, EntityDataSerializersProxy.BOOLEAN, false);
    public static final HappyGhastData<Boolean> StaysStill = new HappyGhastData<>(HappyGhastData.class, EntityDataSerializersProxy.BOOLEAN, false);

    protected HappyGhastData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
