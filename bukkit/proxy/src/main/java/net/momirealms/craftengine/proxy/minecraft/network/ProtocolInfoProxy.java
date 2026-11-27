package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.ProtocolInfo", activeIf = "min_version=1.20.5 && max_version=1.20.6")
public interface ProtocolInfoProxy {
    ProtocolInfoProxy INSTANCE = ASMProxyFactory.create(ProtocolInfoProxy.class);

    @MethodInvoker(name = "codec")
    Object codec(Object target);
}
