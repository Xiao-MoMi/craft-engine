package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.nbt.CompoundTag")
public interface CompoundTagProxy {
    CompoundTagProxy INSTANCE = ASMProxyFactory.create(CompoundTagProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.nbt.CompoundTag");

    @ConstructorInvoker
    Object newInstance();

    @FieldGetter(name = "tags")
    Map<String, Object> getTags(Object target);

    @MethodInvoker(name = "copy")
    Object copy(Object target);

    @MethodInvoker(name = "merge")
    Object merge(Object target, @Type(clazz = CompoundTagProxy.class) Object other);

    @MethodInvoker(name = "get")
    Object get(Object target, String key);

    @MethodInvoker(name = "put")
    Object put(Object target, String key, @Type(clazz = TagProxy.class) Object value);

    @MethodInvoker(name = "putString")
    void putString(Object target, String key, String value);

    @MethodInvoker(name = "remove")
    void remove(Object target, String key);
}
