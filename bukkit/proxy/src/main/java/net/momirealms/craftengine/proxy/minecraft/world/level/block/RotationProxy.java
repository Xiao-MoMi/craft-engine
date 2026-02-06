package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.Rotation")
public interface RotationProxy {
    RotationProxy INSTANCE = ASMProxyFactory.create(RotationProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object NONE = VALUES[0];
    Object CLOCKWISE_90 = VALUES[1];
    Object CLOCKWISE_180 = VALUES[2];
    Object COUNTERCLOCKWISE_90 = VALUES[3];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
