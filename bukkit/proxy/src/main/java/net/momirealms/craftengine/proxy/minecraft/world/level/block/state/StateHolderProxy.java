package net.momirealms.craftengine.proxy.minecraft.world.level.block.state;

import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.PropertyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.StateHolder")
public interface StateHolderProxy {
    StateHolderProxy INSTANCE = ASMProxyFactory.create(StateHolderProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.state.StateHolder");

    @MethodInvoker(name = "hasProperty")
    boolean hasProperty(Object target, @Type(clazz = PropertyProxy.class) Object property);

    @MethodInvoker(name = "getValue")
    <T extends Comparable<T>> T getValue(Object target, @Type(clazz = PropertyProxy.class) Object property);

    @MethodInvoker(name = "setValue")
    Object setValue(Object target, @Type(clazz = PropertyProxy.class) Object property, Comparable<?> value);

    @MethodInvoker(name = "trySetValue")
    Object trySetValue(Object target, @Type(clazz = PropertyProxy.class) Object property, Comparable<?> value);
}
