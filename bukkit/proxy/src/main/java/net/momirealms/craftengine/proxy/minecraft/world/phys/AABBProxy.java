package net.momirealms.craftengine.proxy.minecraft.world.phys;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.phys.AABB")
public interface AABBProxy {
    AABBProxy INSTANCE = ASMProxyFactory.create(AABBProxy.class);

    @ConstructorInvoker
    Object newInstance(double x1, double y1, double z1, double x2, double y2, double z2);

    @FieldGetter(name = "minX")
    double getMinX(Object target);

    @FieldGetter(name = "minY")
    double getMinY(Object target);

    @FieldGetter(name = "minZ")
    double getMinZ(Object target);

    @FieldGetter(name = "maxX")
    double getMaxX(Object target);

    @FieldGetter(name = "maxY")
    double getMaxY(Object target);

    @FieldGetter(name = "maxZ")
    double getMaxZ(Object target);

    @MethodInvoker(name = "setMinX")
    Object setMinX(Object target, double minX);

    @MethodInvoker(name = "setMinY")
    Object setMinY(Object target, double minY);

    @MethodInvoker(name = "setMinZ")
    Object setMinZ(Object target, double minZ);

    @MethodInvoker(name = "setMaxX")
    Object setMaxX(Object target, double maxX);

    @MethodInvoker(name = "setMaxY")
    Object setMaxY(Object target, double maxY);

    @MethodInvoker(name = "setMaxZ")
    Object setMaxZ(Object target, double maxZ);

    @MethodInvoker(name = "move")
    Object move$0(Object target, double x, double y, double z);

    @MethodInvoker(name = "move")
    Object move$1(Object target, @Type(clazz = BlockPosProxy.class) Object blockPos);

    @MethodInvoker(name = "move")
    Object move$2(Object target, @Type(clazz = Vec3Proxy.class) Object vec);
}