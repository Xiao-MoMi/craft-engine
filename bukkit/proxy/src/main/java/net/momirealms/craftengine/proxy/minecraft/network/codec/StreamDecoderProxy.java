package net.momirealms.craftengine.proxy.minecraft.network.codec;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.codec.StreamDecoder", activeIf = "min_version=1.20.5")
public interface StreamDecoderProxy {
    StreamDecoderProxy INSTANCE = ASMProxyFactory.create(StreamDecoderProxy.class);

    @MethodInvoker(name = "decode")
    <T> T decode(Object target, Object buf);
}
