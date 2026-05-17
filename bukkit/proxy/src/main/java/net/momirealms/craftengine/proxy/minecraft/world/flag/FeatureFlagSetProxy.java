package net.momirealms.craftengine.proxy.minecraft.world.flag;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.flag.FeatureFlagSet")
public interface FeatureFlagSetProxy {
    FeatureFlagSetProxy INSTANCE = ASMProxyFactory.create(FeatureFlagSetProxy.class);
}
