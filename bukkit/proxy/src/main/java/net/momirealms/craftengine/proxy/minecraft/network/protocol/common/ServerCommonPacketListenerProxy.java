package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.common.ServerCommonPacketListener", activeIf = "min_version=1.20.2")
public interface ServerCommonPacketListenerProxy {
    ServerCommonPacketListenerProxy INSTANCE = ASMProxyFactory.create(ServerCommonPacketListenerProxy.class);

    @MethodInvoker(name = "handleResourcePackResponse")
    void handleResourcePackResponse(Object target,
                                    @Type(clazz = ServerboundResourcePackPacketProxy.class) Object packet);
}
