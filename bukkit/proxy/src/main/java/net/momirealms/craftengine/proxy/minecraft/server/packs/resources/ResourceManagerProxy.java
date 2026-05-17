package net.momirealms.craftengine.proxy.minecraft.server.packs.resources;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Map;
import java.util.function.Predicate;

@ReflectionProxy(name = "net.minecraft.server.packs.resources.ResourceManager")
public interface ResourceManagerProxy {
    ResourceManagerProxy INSTANCE = ASMProxyFactory.create(ResourceManagerProxy.class);

    @MethodInvoker(name = "listResources")
    Map<Object, Object> listResources(Object target, String path, Predicate<Object> filter);
}
