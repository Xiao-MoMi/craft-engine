package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.Vec3i")
public interface Vec3iProxy {
    Vec3iProxy INSTANCE = ASMProxyFactory.create(Vec3iProxy.class);

    @ConstructorInvoker
    Object newInstance$0(int x, int y, int z); // fixme 无法覆写接口

    @FieldGetter(name = "x")
    int getX(Object target);

    @FieldSetter(name = "x")
    void setX(Object target, int x);

    @FieldGetter(name = "y")
    int getY(Object target);

    @FieldSetter(name = "y")
    void setY(Object target, int y);

    @FieldGetter(name = "z")
    int getZ(Object target);

    @FieldSetter(name = "z")
    void setZ(Object target, int z);
}
