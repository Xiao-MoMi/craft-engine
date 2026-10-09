package net.momirealms.craftengine.proxy.lithium.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.caffeinemc.mods.lithium.common.hopper.LithiumStackList", optional = true)
public interface LithiumStackListProxy {
    LithiumStackListProxy INSTANCE = ASMProxyFactory.create(LithiumStackListProxy.class);
    Class<?> CLASS = SparrowClass.find("net.caffeinemc.mods.lithium.common.hopper.LithiumStackList");

    @MethodInvoker(name = "getModCount")
    long getModCount(Object target);
}
