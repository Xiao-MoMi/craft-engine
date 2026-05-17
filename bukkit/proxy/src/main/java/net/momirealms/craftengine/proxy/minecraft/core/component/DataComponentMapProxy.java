package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentMap", activeIf = "min_version=1.20.5")
public interface DataComponentMapProxy extends DataComponentGetterProxy {
    DataComponentMapProxy INSTANCE = ASMProxyFactory.create(DataComponentMapProxy.class);

    @MethodInvoker(name = "get", activeIf = "max_version=1.21.4")
    <T> T get(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);
}
