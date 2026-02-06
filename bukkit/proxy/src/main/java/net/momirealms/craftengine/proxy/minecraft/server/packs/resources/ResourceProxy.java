package net.momirealms.craftengine.proxy.minecraft.server.packs.resources;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.io.BufferedReader;
import java.io.IOException;

@ReflectionProxy(name = "net.minecraft.server.packs.resources.Resource")
public interface ResourceProxy {
    ResourceProxy INSTANCE = ASMProxyFactory.create(ResourceProxy.class);

    @MethodInvoker(name = "openAsReader")
    BufferedReader openAsReader(Object target) throws IOException;
}
