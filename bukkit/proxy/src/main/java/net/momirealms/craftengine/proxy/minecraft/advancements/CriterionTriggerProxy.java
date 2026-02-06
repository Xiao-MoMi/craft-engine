package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.advancements.CriterionTrigger")
public interface CriterionTriggerProxy {
    CriterionTriggerProxy INSTANCE = ASMProxyFactory.create(CriterionTriggerProxy.class);
}
