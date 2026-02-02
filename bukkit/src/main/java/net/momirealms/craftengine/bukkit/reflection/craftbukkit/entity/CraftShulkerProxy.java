package net.momirealms.craftengine.bukkit.reflection.craftbukkit.entity;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.entity.Shulker;

@ReflectionProxy(name = "org.bukkit.craftbukkit.entity.CraftShulker")
public interface CraftShulkerProxy {
    CraftShulkerProxy INSTANCE = ReflectionHelper.getProxy(CraftShulkerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftShulkerProxy.class);

    @MethodInvoker(name = "getHandle")
    Object getHandle(Shulker instance);
}
