package net.momirealms.craftengine.proxy.minecraft.world.entity.decoration;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.decoration.ArmorStand")
public interface ArmorStandProxy {
    ArmorStandProxy INSTANCE = ASMProxyFactory.create(ArmorStandProxy.class);
    Object DEFAULT_HEAD_POSE = INSTANCE.getDefaultHeadPose();
    Object DEFAULT_BODY_POSE = INSTANCE.getDefaultBodyPose();
    Object DEFAULT_LEFT_ARM_POSE = INSTANCE.getDefaultLeftArmPose();
    Object DEFAULT_RIGHT_ARM_POSE = INSTANCE.getDefaultRightArmPose();
    Object DEFAULT_LEFT_LEG_POSE = INSTANCE.getDefaultLeftLegPose();
    Object DEFAULT_RIGHT_LEG_POSE = INSTANCE.getDefaultRightLegPose();

    @FieldGetter(name = "DEFAULT_HEAD_POSE", isStatic = true)
    Object getDefaultHeadPose();

    @FieldGetter(name = "DEFAULT_BODY_POSE", isStatic = true)
    Object getDefaultBodyPose();

    @FieldGetter(name = "DEFAULT_LEFT_ARM_POSE", isStatic = true)
    Object getDefaultLeftArmPose();

    @FieldGetter(name = "DEFAULT_RIGHT_ARM_POSE", isStatic = true)
    Object getDefaultRightArmPose();

    @FieldGetter(name = "DEFAULT_LEFT_LEG_POSE", isStatic = true)
    Object getDefaultLeftLegPose();

    @FieldGetter(name = "DEFAULT_RIGHT_LEG_POSE", isStatic = true)
    Object getDefaultRightLegPose();
}
