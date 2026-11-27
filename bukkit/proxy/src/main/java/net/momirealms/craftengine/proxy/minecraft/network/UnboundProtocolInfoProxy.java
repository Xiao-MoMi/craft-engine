package net.momirealms.craftengine.proxy.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.function.Function;

@ReflectionProxy(name = "net.minecraft.network.ProtocolInfo$Unbound", activeIf = "min_version=1.20.5 && max_version=1.21.4")
public interface UnboundProtocolInfoProxy {
    UnboundProtocolInfoProxy INSTANCE = ASMProxyFactory.create(UnboundProtocolInfoProxy.class);

    @MethodInvoker(name = "bind", activeIf = "max_version=1.20.6")
    Object bind(Object target, Function<ByteBuf, ByteBuf> bufferFactory);

    @MethodInvoker(name = "listPackets", activeIf = "min_version=1.21")
    void listPackets(Object target, @Type(clazz = UnboundPacketVisitorProxy.class) Object visitor);
}
