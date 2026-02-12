package net.momirealms.craftengine.proxy.minecraft.network.codec;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.codec.StreamCodec", activeIf = "min_version=1.20.5")
public interface StreamCodecProxy extends StreamDecoderProxy, StreamEncoderProxy {
    StreamCodecProxy INSTANCE = ASMProxyFactory.create(StreamCodecProxy.class);
}
