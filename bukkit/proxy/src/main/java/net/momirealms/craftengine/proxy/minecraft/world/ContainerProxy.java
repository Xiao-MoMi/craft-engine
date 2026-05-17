package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.Container")
public interface ContainerProxy {
    ContainerProxy INSTANCE = ASMProxyFactory.create(ContainerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.Container");

    @MethodInvoker(name = "getCurrentRecipe", activeIf = "max_version=1.20.6")
    Object getCurrentRecipe(Object target);

    @MethodInvoker(name = "getContainerSize")
    int getContainerSize(Object target);

    @MethodInvoker(name = "getItem")
    Object getItem(Object target, int index);

    @MethodInvoker(name = "setChanged")
    void setChanged(Object target);

    @MethodInvoker(name = "removeItem")
    Object removeItem(Object target, int index, int count);

    @MethodInvoker(name = "setItem")
    void setItem(Object target, int index, @Type(clazz = ItemStackProxy.class) Object item);
}
