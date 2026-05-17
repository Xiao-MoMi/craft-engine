package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.LightLayer")
public interface LightLayerProxy {
    LightLayerProxy INSTANCE = ASMProxyFactory.create(LightLayerProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Object SKY = VALUES[0];
    Object BLOCK = VALUES[1];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
