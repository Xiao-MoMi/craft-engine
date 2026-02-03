package net.momirealms.craftengine.bukkit.reflection.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(clazz = ByteToMessageDecoder.class)
public interface ByteToMessageDecoderProxy {
    ByteToMessageDecoderProxy INSTANCE = ReflectionHelper.getProxy(ByteToMessageDecoderProxy.class);
    Class<?> CLAZZ = ByteToMessageDecoder.class;

    @MethodInvoker(name = "decode")
    void decode(ByteToMessageDecoder instance, ChannelHandlerContext ctx, ByteBuf in, List<Object> out);
}
