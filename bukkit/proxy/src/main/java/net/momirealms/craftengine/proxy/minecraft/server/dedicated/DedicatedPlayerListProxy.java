package net.momirealms.craftengine.proxy.minecraft.server.dedicated;

import net.momirealms.craftengine.proxy.minecraft.server.players.PlayerListProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.dedicated.DedicatedPlayerList")
public interface DedicatedPlayerListProxy extends PlayerListProxy {
    DedicatedPlayerListProxy INSTANCE = ASMProxyFactory.create(DedicatedPlayerListProxy.class);
}
