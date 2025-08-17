package net.momirealms.craftengine.bukkit.entity.data;

public class VehicleEntityData<T> extends BaseEntityData<T> {
    public static final VehicleEntityData<Integer> Hurt = new VehicleEntityData<>(VehicleEntityData.class, EntityDataValue.Serializers$INT, 0);
    public static final VehicleEntityData<Integer> HurtDir = new VehicleEntityData<>(VehicleEntityData.class, EntityDataValue.Serializers$INT, 1);
    public static final VehicleEntityData<Float> Damage = new VehicleEntityData<>(VehicleEntityData.class, EntityDataValue.Serializers$FLOAT, 0.0F);

    public VehicleEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
