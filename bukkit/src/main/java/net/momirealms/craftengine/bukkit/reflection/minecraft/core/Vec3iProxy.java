package net.momirealms.craftengine.bukkit.reflection.minecraft.core;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.core.Vec3i")
public interface Vec3iProxy {
    Vec3iProxy INSTANCE = ReflectionHelper.getProxy(Vec3iProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(Vec3iProxy.class);

    @ConstructorInvoker
    Object newInstance(int x, int y, int z);

    @FieldGetter(name = "x")
    int x(Object instance);

    @FieldGetter(name = "y")
    int y(Object instance);

    @FieldGetter(name = "z")
    int z(Object instance);

    @FieldSetter(name = "x")
    void x(Object instance, int x);

    @FieldSetter(name = "y")
    void y(Object instance, int y);

    @FieldSetter(name = "z")
    void z(Object instance, int z);

    @MethodInvoker(name = "offset")
    Object offset(Object instance, int x, int y, int z);

    @MethodInvoker(name = "offset")
    Object offset(Object instance, @Type(clazz = Vec3iProxy.class) Object vec3i);

    @MethodInvoker(name = "subtract")
    Object subtract(Object instance, @Type(clazz = Vec3iProxy.class) Object vec3i);

    @MethodInvoker(name = "multiply")
    Object multiply(Object instance, int scale);

    @MethodInvoker(name = "above")
    Object above(Object instance);

    @MethodInvoker(name = "above")
    Object above(Object instance, int distance);

    @MethodInvoker(name = "below")
    Object below(Object instance);

    @MethodInvoker(name = "below")
    Object below(Object instance, int distance);

    @MethodInvoker(name = "north")
    Object north(Object instance);

    @MethodInvoker(name = "north")
    Object north(Object instance, int distance);

    @MethodInvoker(name = "south")
    Object south(Object instance);

    @MethodInvoker(name = "south")
    Object south(Object instance, int distance);

    @MethodInvoker(name = "west")
    Object west(Object instance);

    @MethodInvoker(name = "west")
    Object west(Object instance, int distance);

    @MethodInvoker(name = "east")
    Object east(Object instance);

    @MethodInvoker(name = "east")
    Object east(Object instance, int distance);
}
