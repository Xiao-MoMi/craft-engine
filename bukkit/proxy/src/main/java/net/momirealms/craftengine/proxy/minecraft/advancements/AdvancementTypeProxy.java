package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.advancements.AdvancementType")
public interface AdvancementTypeProxy {
    AdvancementTypeProxy INSTANCE = ASMProxyFactory.create(AdvancementTypeProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> TASK = VALUES[0];
    Enum<?> CHALLENGE = VALUES[1];
    Enum<?> GOAL = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
