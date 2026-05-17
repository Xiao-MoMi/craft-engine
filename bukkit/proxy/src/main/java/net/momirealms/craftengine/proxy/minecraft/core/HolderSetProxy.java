package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.core.HolderSet")
public interface HolderSetProxy {
    HolderSetProxy INSTANCE = ASMProxyFactory.create(HolderSetProxy.class);

    @ReflectionProxy(name = "net.minecraft.core.HolderSet$Named")
    interface NamedProxy extends HolderSetProxy {
        NamedProxy INSTANCE = ASMProxyFactory.create(NamedProxy.class);

        @MethodInvoker(name = "bind")
        void bind(Object target, List<Object> contents);

        @MethodInvoker(name = "contents")
        List<Object> contents(Object target);
    }
}
