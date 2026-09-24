package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.entity.PositionPath", activeIf = "min_version=26.3")
public interface PositionPathProxy {
    PositionPathProxy INSTANCE = ASMProxyFactory.create(PositionPathProxy.class);
    @MethodInvoker(name = "of", isStatic = true)
    Object of(@Type(clazz = net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy.class) Object position);
}
