package net.momirealms.craftengine.proxy.minecraft.server.network;

import io.netty.channel.ChannelFuture;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.server.network.ServerConnectionListener")
public interface ServerConnectionListenerProxy {
    ServerConnectionListenerProxy INSTANCE = ASMProxyFactory.create(ServerConnectionListenerProxy.class);

    @FieldGetter(name = "channels")
    List<ChannelFuture> getChannels(Object target);

    @FieldSetter(name = "channels")
    void setChannels(Object target, List<ChannelFuture> channels);
}
