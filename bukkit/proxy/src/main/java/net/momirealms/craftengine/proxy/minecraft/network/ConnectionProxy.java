package net.momirealms.craftengine.proxy.minecraft.network;

import io.netty.channel.Channel;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.network.Connection")
public interface ConnectionProxy {
    ConnectionProxy INSTANCE = ASMProxyFactory.create(ConnectionProxy.class);

    @MethodInvoker(name = "disconnect")
    void disconnect(Object target, @Type(clazz = ComponentProxy.class) Object reason);

    @MethodInvoker(name = "handleDisconnection")
    void handleDisconnection(Object target);

    @FieldGetter(name = "packetListener")
    Object getPacketListener(Object target);

    @FieldSetter(name = "packetListener")
    void setPacketListener(Object target, Object packetListener);

    @FieldGetter(name = "channel")
    Channel getChannel(Object target);

    @FieldSetter(name = "channel")
    void setChannel(Object target, Channel channel);
}
