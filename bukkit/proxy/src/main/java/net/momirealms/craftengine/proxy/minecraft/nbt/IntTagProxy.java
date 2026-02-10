package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.nbt.IntTag")
public interface IntTagProxy {
    IntTagProxy INSTANCE = ASMProxyFactory.create(IntTagProxy.class);

    @MethodInvoker(name = "valueOf", isStatic = true)
    Object valueOf(int value);
}
