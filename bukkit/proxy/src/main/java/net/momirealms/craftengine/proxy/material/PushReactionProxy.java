package net.momirealms.craftengine.proxy.material;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.material.PushReaction")
public interface PushReactionProxy {
    PushReactionProxy INSTANCE = ASMProxyFactory.create(PushReactionProxy.class);
    Object[] VALUES = INSTANCE.values();

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
