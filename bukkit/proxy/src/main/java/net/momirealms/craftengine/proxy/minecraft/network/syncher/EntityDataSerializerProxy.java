package net.momirealms.craftengine.proxy.minecraft.network.syncher;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.syncher.EntityDataSerializer")
public interface EntityDataSerializerProxy {
    EntityDataSerializerProxy INSTANCE = ASMProxyFactory.create(EntityDataSerializerProxy.class);
}
