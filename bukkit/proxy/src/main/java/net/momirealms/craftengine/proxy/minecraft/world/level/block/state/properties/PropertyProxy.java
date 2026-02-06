package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.properties.Property")
public interface PropertyProxy {
    PropertyProxy INSTANCE = ASMProxyFactory.create(PropertyProxy.class);
    Class<?> CLASS = SparrowClass.find("wnet.minecraft.world.level.block.state.properties.Property");

    @MethodInvoker(name = "getName")
    String getName(Object target);
}
