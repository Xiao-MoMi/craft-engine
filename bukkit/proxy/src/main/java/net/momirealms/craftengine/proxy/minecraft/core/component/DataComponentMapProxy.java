package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentMap", activeIf = "min_version=1.20.5")
public interface DataComponentMapProxy {
    DataComponentMapProxy INSTANCE = ASMProxyFactory.create(DataComponentMapProxy.class);
}
