package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.AbstractList;

@ReflectionProxy(name = "net.minecraft.nbt.ListTag")
public interface ListTagProxy {
    ListTagProxy INSTANCE = ASMProxyFactory.create(ListTagProxy.class);

    @ConstructorInvoker
    AbstractList<Object> newInstance();

    @MethodInvoker(name = "get")
    Object get(Object target, int index);

    @MethodInvoker(name = "addTag")
    void addTag(Object target, int index, @Type(clazz = TagProxy.class) Object value);

    @MethodInvoker(name = "remove")
    void remove(Object target, int index);
}
