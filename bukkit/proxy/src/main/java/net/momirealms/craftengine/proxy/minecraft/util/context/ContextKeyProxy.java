package net.momirealms.craftengine.proxy.minecraft.util.context;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.context.ContextKey")
public interface ContextKeyProxy {
    ContextKeyProxy INSTANCE = ASMProxyFactory.create(ContextKeyProxy.class);
}
