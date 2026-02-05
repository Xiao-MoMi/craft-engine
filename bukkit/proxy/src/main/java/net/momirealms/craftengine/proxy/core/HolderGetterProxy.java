package net.momirealms.craftengine.proxy.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.HolderGetter")
public interface HolderGetterProxy {
    HolderGetterProxy INSTANCE = ASMProxyFactory.create(HolderLookupProxy.class);

    @ReflectionProxy(name = "net.minecraft.core.HolderGetter$Provider")
    interface ProviderProxy {
        ProviderProxy INSTANCE = ASMProxyFactory.create(ProviderProxy.class);
    }
}
