package net.momirealms.craftengine.proxy.minecraft.nbt;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.nbt.StringTag")
public interface StringTagProxy {
    StringTagProxy INSTANCE = ASMProxyFactory.create(StringTagProxy.class);

    @MethodInvoker(name = "valueOf", isStatic = true)
    Object valueOf(String value);
}
