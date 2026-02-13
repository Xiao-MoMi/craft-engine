package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.Rotation")
public interface RotationProxy {
    RotationProxy INSTANCE = ASMProxyFactory.create(RotationProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.Rotation");
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> NONE = VALUES[0];
    Enum<?> CLOCKWISE_90 = VALUES[1];
    Enum<?> CLOCKWISE_180 = VALUES[2];
    Enum<?> COUNTERCLOCKWISE_90 = VALUES[3];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
