package net.momirealms.craftengine.proxy.minecraft.server.network.config;

import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.server.network.config.ServerResourcePackConfigurationTask", activeIf = "min_version=1.20.2")
public interface ServerResourcePackConfigurationTaskProxy {
    ServerResourcePackConfigurationTaskProxy INSTANCE = ASMProxyFactory.create(ServerResourcePackConfigurationTaskProxy.class);
    Object TYPE = INSTANCE != null ? INSTANCE.getType() : null;

    @ConstructorInvoker
    Object newInstance(@Type(clazz = MinecraftServerProxy.ServerResourcePackInfoProxy.class) Object packProperties);

    @FieldGetter(name = "TYPE", isStatic = true)
    Object getType();
}
