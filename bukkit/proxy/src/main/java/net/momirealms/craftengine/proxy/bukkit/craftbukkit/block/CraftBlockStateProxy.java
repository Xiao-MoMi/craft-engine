package net.momirealms.craftengine.proxy.bukkit.craftbukkit.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlockState")
public interface CraftBlockStateProxy {
    CraftBlockStateProxy INSTANCE = ASMProxyFactory.create(CraftBlockStateProxy.class);

    @MethodInvoker(name = "getHandle")
    Object getHandle(Object target);
}
