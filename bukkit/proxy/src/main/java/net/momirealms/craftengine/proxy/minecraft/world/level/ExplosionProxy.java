package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.Explosion")
public interface ExplosionProxy {
    ExplosionProxy INSTANCE = ASMProxyFactory.create(ExplosionProxy.class);

    @MethodInvoker(name = "canTriggerBlocks", activeIf = "min_version=1.21")
    boolean canTriggerBlocks(Object target);
}
