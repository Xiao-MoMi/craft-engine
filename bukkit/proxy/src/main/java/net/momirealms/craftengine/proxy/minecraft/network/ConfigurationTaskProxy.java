package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.network.ConfigurationTask", activeIf = "min_version=1.20.2")
public interface ConfigurationTaskProxy {
    ConfigurationTaskProxy INSTANCE = ASMProxyFactory.create(ConfigurationTaskProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.server.network.ConfigurationTask");

    @ReflectionProxy(name = "net.minecraft.server.network.ConfigurationTask$Type", activeIf = "min_version=1.20.2")
    interface TypeProxy {
        TypeProxy INSTANCE = ASMProxyFactory.create(TypeProxy.class);
    }
}
