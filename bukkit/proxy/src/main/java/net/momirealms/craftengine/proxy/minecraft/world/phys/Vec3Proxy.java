package net.momirealms.craftengine.proxy.minecraft.world.phys;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.phys.Vec3")
public interface Vec3Proxy {
    Vec3Proxy INSTANCE = ASMProxyFactory.create(Vec3Proxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.phys.Vec3");
    Object ZERO = INSTANCE.getZero();

    @ConstructorInvoker
    Object newInstance(double x, double y, double z);

    @FieldGetter(name = "ZERO", isStatic = true)
    Object getZero();

    @FieldGetter(name = "x")
    double getX(Object target);

    @FieldGetter(name = "y")
    double getY(Object target);

    @FieldGetter(name = "z")
    double getZ(Object target);

    @FieldSetter(name = "x")
    void setX(Object target, double x);

    @FieldSetter(name = "y")
    void setY(Object target, double y);

    @FieldSetter(name = "z")
    void setZ(Object target, double z);

    @MethodInvoker(name = "add")
    Object add(Object target, double x, double y, double z);

    @MethodInvoker(name = "add")
    Object add(Object target, @Type(clazz = Vec3Proxy.class) Object vec);

    @MethodInvoker(name = "subtract")
    Object subtract(Object target, double x, double y, double z);

    @MethodInvoker(name = "subtract")
    Object subtract(Object target, @Type(clazz = Vec3Proxy.class) Object vec);

    @MethodInvoker(name = "multiply")
    Object multiply(Object target, double factorX, double factorY, double factorZ);
}
