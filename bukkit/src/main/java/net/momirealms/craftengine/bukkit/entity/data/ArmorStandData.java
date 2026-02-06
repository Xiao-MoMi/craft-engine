package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.proxy.minecraft.world.entity.decoration.ArmorStandProxy;

public class ArmorStandData<T> extends LivingEntityData<T> {
    public static final ArmorStandData<Byte> ArmorStandFlags = new ArmorStandData<>(ArmorStandData.class, EntityDataValue.Serializers$BYTE, (byte) 0);
    // rotations
    public static final ArmorStandData<Object> HeadPose = new ArmorStandData<>(ArmorStandData.class, EntityDataValue.Serializers$ROTATIONS, ArmorStandProxy.DEFAULT_HEAD_POSE);
    public static final ArmorStandData<Object> BodyPose = new ArmorStandData<>(ArmorStandData.class, EntityDataValue.Serializers$ROTATIONS, ArmorStandProxy.DEFAULT_BODY_POSE);
    public static final ArmorStandData<Object> LeftArmPose = new ArmorStandData<>(ArmorStandData.class, EntityDataValue.Serializers$ROTATIONS, ArmorStandProxy.DEFAULT_LEFT_ARM_POSE);
    public static final ArmorStandData<Object> RightArmPose = new ArmorStandData<>(ArmorStandData.class, EntityDataValue.Serializers$ROTATIONS, ArmorStandProxy.DEFAULT_RIGHT_ARM_POSE);
    public static final ArmorStandData<Object> LeftLegPose = new ArmorStandData<>(ArmorStandData.class, EntityDataValue.Serializers$ROTATIONS, ArmorStandProxy.DEFAULT_LEFT_LEG_POSE);
    public static final ArmorStandData<Object> RightLegPose = new ArmorStandData<>(ArmorStandData.class, EntityDataValue.Serializers$ROTATIONS, ArmorStandProxy.DEFAULT_RIGHT_LEG_POSE);

    public ArmorStandData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}