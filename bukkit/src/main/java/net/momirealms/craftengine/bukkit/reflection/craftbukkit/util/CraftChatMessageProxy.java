package net.momirealms.craftengine.bukkit.reflection.craftbukkit.util;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.util.CraftChatMessage")
public interface CraftChatMessageProxy {
    CraftChatMessageProxy INSTANCE = ReflectionHelper.getProxy(CraftChatMessageProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftChatMessageProxy.class);

    @MethodInvoker(name = "fromJSON", isStatic = true)
    Object fromJSON(String jsonMessage);
}
