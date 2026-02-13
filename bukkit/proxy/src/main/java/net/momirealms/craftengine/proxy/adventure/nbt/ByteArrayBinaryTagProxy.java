package net.momirealms.craftengine.proxy.adventure.nbt;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}nbt{}ByteArrayBinaryTag", ignoreRelocation = true)
public interface ByteArrayBinaryTagProxy {
    ByteArrayBinaryTagProxy INSTANCE = ASMProxyFactory.create(ByteArrayBinaryTagProxy.class);

    @MethodInvoker(name = "byteArrayBinaryTag", isStatic = true)
    Object byteArrayBinaryTag(byte... value);

    @MethodInvoker(name = "value")
    byte[] value(Object target);
}
