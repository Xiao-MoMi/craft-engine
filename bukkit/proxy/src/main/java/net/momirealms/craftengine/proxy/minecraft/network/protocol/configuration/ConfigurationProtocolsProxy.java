package net.momirealms.craftengine.proxy.minecraft.network.protocol.configuration;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.configuration.ConfigurationProtocols", activeIf = "min_version=1.20.5")
public interface ConfigurationProtocolsProxy {
    ConfigurationProtocolsProxy INSTANCE = ASMProxyFactory.create(ConfigurationProtocolsProxy.class);

    @FieldGetter(name = "CLIENTBOUND", isStatic = true, activeIf = "max_version=1.20.6")
    Object clientbound$legacy();

    @FieldGetter(name = "CLIENTBOUND_TEMPLATE", isStatic = true, activeIf = "min_version=1.21")
    Object clientbound();

    @FieldGetter(name = "SERVERBOUND", isStatic = true, activeIf = "max_version=1.20.6")
    Object serverbound$legacy();

    @FieldGetter(name = "SERVERBOUND_TEMPLATE", isStatic = true, activeIf = "min_version=1.21")
    Object serverbound();
}
