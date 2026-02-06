package net.momirealms.craftengine.proxy.minecraft.network.config;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.network.config.ServerResourcePackConfigurationTask")
public interface ServerResourcePackConfigurationTaskProxy {
    ServerResourcePackConfigurationTaskProxy INSTANCE = ASMProxyFactory.create(ServerResourcePackConfigurationTaskProxy.class);
    Object TYPE = INSTANCE.getType();

    @FieldGetter(name = "TYPE", isStatic = true)
    Object getType();
}
