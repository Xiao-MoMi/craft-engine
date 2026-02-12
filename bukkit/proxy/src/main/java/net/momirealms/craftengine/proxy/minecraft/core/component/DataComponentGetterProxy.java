package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentGetter", activeIf = "min_version=1.21.5")
public interface DataComponentGetterProxy {
    DataComponentGetterProxy INSTANCE = ASMProxyFactory.create(DataComponentGetterProxy.class);

    @MethodInvoker(name = "get")
    <T> T get(Object target, @Type(clazz = DataComponentTypeProxy.class) Object type);
}
