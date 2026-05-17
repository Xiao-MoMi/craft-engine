package net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.pathfinder.PathComputationType")
public interface PathComputationTypeProxy {
    PathComputationTypeProxy INSTANCE = ASMProxyFactory.create(PathComputationTypeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.pathfinder.PathComputationType");
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> LAND = VALUES[0];
    Enum<?> WATER = VALUES[1];
    Enum<?> AIR = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
