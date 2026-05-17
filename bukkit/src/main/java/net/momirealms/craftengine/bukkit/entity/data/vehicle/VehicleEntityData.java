package net.momirealms.craftengine.bukkit.entity.data.vehicle;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

public class VehicleEntityData<T> extends BaseEntityData<T> {
    public static final VehicleEntityData<Integer> HurtTime = new VehicleEntityData<>(VehicleEntityData.class, EntityDataSerializersProxy.INT, 0);
    public static final VehicleEntityData<Integer> HurtDir = new VehicleEntityData<>(VehicleEntityData.class, EntityDataSerializersProxy.INT, 1);
    public static final VehicleEntityData<Float> Damage = new VehicleEntityData<>(VehicleEntityData.class, EntityDataSerializersProxy.FLOAT, 0.0F);

    protected VehicleEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
