package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.FallingBlock")
public interface FallingBlockProxy {
    FallingBlockProxy INSTANCE = ASMProxyFactory.create(FallingBlockProxy.class);

    @MethodInvoker(name = "isFree", isStatic = true)
    boolean isFree(Object state);
}
