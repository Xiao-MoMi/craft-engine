package net.momirealms.craftengine.proxy.minecraft.network.codec;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.codec.StreamEncoder", activeIf = "min_version=1.20.5")
public interface StreamEncoderProxy {
    StreamEncoderProxy INSTANCE = ASMProxyFactory.create(StreamEncoderProxy.class);

    @MethodInvoker(name = "encode")
    void encode(Object target, Object buf, Object value);
}
