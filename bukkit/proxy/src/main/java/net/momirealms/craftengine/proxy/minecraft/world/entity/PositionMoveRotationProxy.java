package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.entity.PositionMoveRotation", activeIf = "min_version=1.21.2")
public interface PositionMoveRotationProxy {
    PositionMoveRotationProxy INSTANCE = ASMProxyFactory.create(PositionMoveRotationProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = Vec3Proxy.class) Object position, @Type(clazz = Vec3Proxy.class) Object deltaMovement, float yRot, float xRot);
}
