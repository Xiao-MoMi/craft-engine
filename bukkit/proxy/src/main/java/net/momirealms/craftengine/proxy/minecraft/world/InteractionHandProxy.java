package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.InteractionHand")
public interface InteractionHandProxy {
    InteractionHandProxy INSTANCE = ASMProxyFactory.create(InteractionHandProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> MAIN_HAND = VALUES[0];
    Enum<?> OFF_HAND = VALUES[1];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
