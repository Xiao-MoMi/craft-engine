package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.entity.PositionMoveRotation", activeIf = "min_version=1.21.2")
public interface PositionMoveRotationProxy {
    PositionMoveRotationProxy INSTANCE = ASMProxyFactory.create(PositionMoveRotationProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = Vec3Proxy.class) Object position, @Type(clazz = Vec3Proxy.class) Object deltaMovement, float yRot, float xRot);

    @FieldGetter(name = "yRot")
    float getYRot(Object target);

    @FieldGetter(name = "xRot")
    float getXRot(Object target);

    @FieldSetter(name = "yRot")
    void setYRot(Object target, float yRot);

    @FieldSetter(name = "xRot")
    void setXRot(Object target, float xRot);
}
