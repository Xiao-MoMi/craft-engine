package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.nbt.ByteTag")
public interface ByteTagProxy {
    ByteTagProxy INSTANCE = ASMProxyFactory.create(ByteTagProxy.class);

    @MethodInvoker(name = "valueOf", isStatic = true)
    Object valueOf(byte value);

    @MethodInvoker(name = "value")
    byte value(Object target);
}
