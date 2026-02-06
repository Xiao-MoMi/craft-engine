package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.InteractionHand")
public interface InteractionHandProxy {
    InteractionHandProxy INSTANCE = ASMProxyFactory.create(InteractionHandProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object MAIN_HAND = VALUES[0];
    Object OFF_HAND = VALUES[1];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
