package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.component.PatchedDataComponentMap", activeIf = "min_version=1.20.5")
public interface PatchedDataComponentMapProxy {
    PatchedDataComponentMapProxy INSTANCE = ASMProxyFactory.create(PatchedDataComponentMapProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = DataComponentMapProxy.class) Object prototype);

    @MethodInvoker(name = "setAll")
    void setAll(Object target, @Type(clazz = DataComponentMapProxy.class) Object components);

    @MethodInvoker(name = "remove")
    Object remove(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);
}
