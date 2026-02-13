package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.HolderLookup")
public interface HolderLookupProxy extends HolderGetterProxy {
    HolderLookupProxy INSTANCE = ASMProxyFactory.create(HolderLookupProxy.class);

    @ReflectionProxy(name = "net.minecraft.core.HolderLookup$Provider")
    interface ProviderProxy extends HolderGetterProxy.ProviderProxy {
        ProviderProxy INSTANCE = ASMProxyFactory.create(ProviderProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.core.HolderLookup$Provider");
    }
}
