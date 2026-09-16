package net.momirealms.craftengine.proxy.minecraft.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

@ReflectionProxy(name = "net.minecraft.network.Connection")
public interface ConnectionProxy {
    ConnectionProxy INSTANCE = ASMProxyFactory.create(ConnectionProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.Connection");

    @MethodInvoker(name = "disconnect")
    void disconnect(Object target, @Type(clazz = ComponentProxy.class) Object reason);

    @MethodInvoker(name = "handleDisconnection")
    void handleDisconnection(Object target);

    @FieldGetter(name = "packetListener")
    Object getPacketListener(Object target);

    @FieldSetter(name = "packetListener")
    void setPacketListener(Object target, Object packetListener);

    // Paper 在配置阶段记录最近一次资源包状态；没有此扩展字段的服务端使用空实现。
    @FieldSetter(name = "resourcePackStatus", activeIf = "has_patch=paper", optional = true)
    default void setResourcePackStatus(Object target, PlayerResourcePackStatusEvent.Status status) {
    }

    @FieldGetter(name = "channel")
    Channel getChannel(Object target);

    @FieldSetter(name = "channel")
    void setChannel(Object target, Channel channel);

    @MethodInvoker(name = "send", activeIf = "min_version=1.21.6")
    void send$0(Object target, @Type(clazz = PacketProxy.class) Object packet, @Nullable ChannelFutureListener sendListener);

    @MethodInvoker(name = "send", activeIf = "max_version=1.21.5")
    void send$1(Object target, @Type(clazz = PacketProxy.class) Object packet, @Nullable @Type(clazz = PacketSendListenerProxy.class) Object sendListener);
}
