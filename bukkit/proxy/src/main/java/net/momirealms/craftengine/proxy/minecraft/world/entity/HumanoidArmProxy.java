package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.HumanoidArm")
public interface HumanoidArmProxy {
    HumanoidArmProxy INSTANCE = ASMProxyFactory.create(HumanoidArmProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object LEFT = VALUES[0];
    Object RIGHT = VALUES[1];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
