package net.momirealms.craftengine.proxy.minecraft.world.level.material;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.material.PushReaction")
public interface PushReactionProxy {
    PushReactionProxy INSTANCE = ASMProxyFactory.create(PushReactionProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
