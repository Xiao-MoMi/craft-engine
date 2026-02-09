package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = {"net.minecraft.core.component.DataComponentExactPredicate", "net.minecraft.core.component.DataComponentPredicate"}, activeIf = "min_version=1.20.5")
public interface DataComponentExactPredicateProxy {
    DataComponentExactPredicateProxy INSTANCE = ASMProxyFactory.create(DataComponentExactPredicateProxy.class);

    @MethodInvoker(name = "allOf", isStatic = true)
    Object allOf(@Type(clazz = DataComponentMapProxy.class) Object map);
}
