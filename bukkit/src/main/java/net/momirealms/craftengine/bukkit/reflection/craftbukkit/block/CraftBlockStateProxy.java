package net.momirealms.craftengine.bukkit.reflection.craftbukkit.block;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.block.CraftBlockState")
public interface CraftBlockStateProxy {
    CraftBlockStateProxy INSTANCE = ReflectionHelper.getProxy(CraftBlockStateProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftBlockStateProxy.class);

    @MethodInvoker(name = "getHandle")
    Object getHandle(Object instance);
}
