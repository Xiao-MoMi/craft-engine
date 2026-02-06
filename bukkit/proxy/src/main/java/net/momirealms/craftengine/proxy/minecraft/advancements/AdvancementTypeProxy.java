package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.advancements.AdvancementType")
public interface AdvancementTypeProxy {
    AdvancementTypeProxy INSTANCE = ASMProxyFactory.create(AdvancementTypeProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object TASK = VALUES[0];
    Object CHALLENGE = VALUES[1];
    Object GOAL = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
