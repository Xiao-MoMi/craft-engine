package net.momirealms.craftengine.bukkit.reflection.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(clazz = MessageToByteEncoder.class)
public interface MessageToByteEncoderProxy {
    MessageToByteEncoderProxy INSTANCE = ReflectionHelper.getProxy(MessageToByteEncoderProxy.class);
    Class<?> CLAZZ = MessageToByteEncoder.class;

    @MethodInvoker(name = "encode")
    void encode(MessageToByteEncoder<?> instance, ChannelHandlerContext ctx, Object msg, ByteBuf out);
}
