package net.momirealms.craftengine.proxy.bukkit.craftbukkit;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftServer")
public interface CraftServerProxy {
    CraftServerProxy INSTANCE = ASMProxyFactory.create(CraftServerProxy.class);

    @FieldGetter(name = "playerList")
    Object getPlayerList(Object target);
}
