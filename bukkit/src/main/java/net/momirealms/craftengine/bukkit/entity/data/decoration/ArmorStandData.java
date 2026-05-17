package net.momirealms.craftengine.bukkit.entity.data.decoration;

import net.momirealms.craftengine.bukkit.entity.data.LivingEntityData;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.decoration.ArmorStandProxy;

public class ArmorStandData<T> extends LivingEntityData<T> {
    public static final ArmorStandData<Byte> ClientFlags = new ArmorStandData<>(ArmorStandData.class, EntityDataSerializersProxy.BYTE, (byte) 0);
    // rotations
    public static final ArmorStandData<Object> HeadPose = new ArmorStandData<>(ArmorStandData.class, EntityDataSerializersProxy.ROTATIONS, ArmorStandProxy.DEFAULT_HEAD_POSE);
    public static final ArmorStandData<Object> BodyPose = new ArmorStandData<>(ArmorStandData.class, EntityDataSerializersProxy.ROTATIONS, ArmorStandProxy.DEFAULT_BODY_POSE);
    public static final ArmorStandData<Object> LeftArmPose = new ArmorStandData<>(ArmorStandData.class, EntityDataSerializersProxy.ROTATIONS, ArmorStandProxy.DEFAULT_LEFT_ARM_POSE);
    public static final ArmorStandData<Object> RightArmPose = new ArmorStandData<>(ArmorStandData.class, EntityDataSerializersProxy.ROTATIONS, ArmorStandProxy.DEFAULT_RIGHT_ARM_POSE);
    public static final ArmorStandData<Object> LeftLegPose = new ArmorStandData<>(ArmorStandData.class, EntityDataSerializersProxy.ROTATIONS, ArmorStandProxy.DEFAULT_LEFT_LEG_POSE);
    public static final ArmorStandData<Object> RightLegPose = new ArmorStandData<>(ArmorStandData.class, EntityDataSerializersProxy.ROTATIONS, ArmorStandProxy.DEFAULT_RIGHT_LEG_POSE);

    protected ArmorStandData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}