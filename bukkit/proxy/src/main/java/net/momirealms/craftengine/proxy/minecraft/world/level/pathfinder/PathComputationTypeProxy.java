package net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.pathfinder.PathComputationType")
public interface PathComputationTypeProxy {
    PathComputationTypeProxy INSTANCE = ASMProxyFactory.create(PathComputationTypeProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object LAND = VALUES[0];
    Object WATER = VALUES[1];
    Object AIR = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
