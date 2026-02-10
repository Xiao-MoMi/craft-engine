package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.core.HolderGetter")
public interface HolderGetterProxy {
    HolderGetterProxy INSTANCE = ASMProxyFactory.create(HolderLookupProxy.class);

    @MethodInvoker(name = "get")
    Optional<Object> get$1(Object target, @Type(clazz = ResourceKeyProxy.class) Object key);

    @ReflectionProxy(name = "net.minecraft.core.HolderGetter$Provider")
    interface ProviderProxy {
        ProviderProxy INSTANCE = ASMProxyFactory.create(ProviderProxy.class);
    }
}
