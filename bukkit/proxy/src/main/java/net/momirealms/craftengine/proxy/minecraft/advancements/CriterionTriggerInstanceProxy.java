package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.advancements.CriterionTriggerInstance")
public interface CriterionTriggerInstanceProxy {
    CriterionTriggerInstanceProxy INSTANCE = ASMProxyFactory.create(CriterionTriggerInstanceProxy.class);
}
