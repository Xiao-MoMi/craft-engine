package net.momirealms.craftengine.proxy.minecraft.advancements.criterion;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.advancements.criterion.ImpossibleTrigger", "net.minecraft.advancements.critereon.ImpossibleTrigger"})
public interface ImpossibleTriggerProxy {
    ImpossibleTriggerProxy INSTANCE = ASMProxyFactory.create(ImpossibleTriggerProxy.class);

    @ConstructorInvoker
    Object newInstance();

    @ReflectionProxy(name = {"net.minecraft.advancements.criterion.ImpossibleTrigger$TriggerInstance", "net.minecraft.advancements.critereon.ImpossibleTrigger$TriggerInstance"})
    interface TriggerInstanceProxy {
        TriggerInstanceProxy INSTANCE = ASMProxyFactory.create(TriggerInstanceProxy.class);

        @ConstructorInvoker
        Object newInstance();
    }
}
