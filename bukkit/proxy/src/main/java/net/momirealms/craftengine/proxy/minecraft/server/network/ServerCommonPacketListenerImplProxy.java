package net.momirealms.craftengine.proxy.minecraft.server.network;

import io.netty.channel.SimpleChannelInboundHandler;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.network.ServerCommonPacketListenerImpl", activeIf = "min_version=1.20.2")
public interface ServerCommonPacketListenerImplProxy extends ServerPlayerConnectionProxy {
    ServerCommonPacketListenerImplProxy INSTANCE = ASMProxyFactory.create(ServerCommonPacketListenerImplProxy.class);

    @FieldGetter(name = "connection", activeIf = "min_version=1.20.2")
    SimpleChannelInboundHandler<Object> getConnection(Object target);

    @FieldGetter(name = "closed", activeIf = "min_version=1.20.5")
    boolean isClosed(Object target);

    @FieldSetter(name = "closed", activeIf = "min_version=1.20.5")
    void setClosed(Object target, boolean closed);

}
