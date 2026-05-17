package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.HumanoidArm")
public interface HumanoidArmProxy {
    HumanoidArmProxy INSTANCE = ASMProxyFactory.create(HumanoidArmProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> LEFT = VALUES[0];
    Enum<?> RIGHT = VALUES[1];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
