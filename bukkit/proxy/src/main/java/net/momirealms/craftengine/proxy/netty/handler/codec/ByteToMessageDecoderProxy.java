package net.momirealms.craftengine.proxy.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(clazz = ByteToMessageDecoder.class)
public interface ByteToMessageDecoderProxy {
    ByteToMessageDecoderProxy INSTANCE = ASMProxyFactory.create(ByteToMessageDecoderProxy.class);

    @MethodInvoker(name = "decode")
    void decode(Object target, ChannelHandlerContext ctx, ByteBuf in, List<Object> out);
}
