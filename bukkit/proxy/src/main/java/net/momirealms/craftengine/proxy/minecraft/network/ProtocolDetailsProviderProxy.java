package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.ProtocolInfo$DetailsProvider", activeIf = "min_version=1.21.5")
public interface ProtocolDetailsProviderProxy {
    ProtocolDetailsProviderProxy INSTANCE = ASMProxyFactory.create(ProtocolDetailsProviderProxy.class);

    @MethodInvoker(name = "details")
    Object details(Object target);
}
