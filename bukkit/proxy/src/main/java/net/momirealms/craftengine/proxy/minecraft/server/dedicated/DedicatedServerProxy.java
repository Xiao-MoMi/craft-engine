package net.momirealms.craftengine.proxy.minecraft.server.dedicated;

import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.dedicated.DedicatedServer")
public interface DedicatedServerProxy extends MinecraftServerProxy {
    DedicatedServerProxy INSTANCE = ASMProxyFactory.create(DedicatedServerProxy.class);

    @FieldGetter(name = "settings")
    Object getSettings(Object target);
}
