package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.advancements.Criterion")
public interface CriterionProxy {
    CriterionProxy INSTANCE = ASMProxyFactory.create(CriterionProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.2")
    Object newInstance(@Type(clazz = CriterionTriggerProxy.class) Object trigger, @Type(clazz = CriterionTriggerInstanceProxy.class) Object triggerInstance);

    @ConstructorInvoker(activeIf = "max_version=1.20.1")
    Object newInstance(@Type(clazz = CriterionTriggerInstanceProxy.class) Object triggerInstance);
}
