package net.momirealms.craftengine.proxy.material;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.material.MapColor")
public interface MapColorProxy {
    MapColorProxy INSTANCE = ASMProxyFactory.create(MapColorProxy.class);

    @MethodInvoker(name = "byId", isStatic = true)
    Object byId(int id);
}
