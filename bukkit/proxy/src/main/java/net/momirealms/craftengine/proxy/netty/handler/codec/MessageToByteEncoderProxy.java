package net.momirealms.craftengine.proxy.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(clazz = MessageToByteEncoder.class)
public interface MessageToByteEncoderProxy {
    MessageToByteEncoderProxy INSTANCE = ASMProxyFactory.create(MessageToByteEncoderProxy.class);

    @MethodInvoker(name = "encode")
    void encode(Object target, ChannelHandlerContext ctx, Object msg, ByteBuf out);
}
