package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentHolder", activeIf = "min_version=1.20.5")
public interface DataComponentHolderProxy {
    DataComponentHolderProxy INSTANCE = ASMProxyFactory.create(DataComponentHolderProxy.class);

    @MethodInvoker(name = "getComponents", activeIf = "min_version=1.20.5")
    Object getComponents(Object target);

    @MethodInvoker(name = "get", activeIf = "min_version=1.20.5")
    <T> T get(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);

    @MethodInvoker(name = "has", activeIf = "min_version=1.20.5")
    boolean has(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);
}
