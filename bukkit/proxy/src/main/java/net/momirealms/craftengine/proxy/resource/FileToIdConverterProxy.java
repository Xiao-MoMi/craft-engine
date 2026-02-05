package net.momirealms.craftengine.proxy.resource;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.resources.FileToIdConverter")
public interface FileToIdConverterProxy {
    FileToIdConverterProxy INSTANCE = ASMProxyFactory.create(FileToIdConverterProxy.class);

    @MethodInvoker(name = "json", isStatic = true)
    Object json(String name);

    @MethodInvoker(name = "listMatchingResources")
    Map<Object, Object> listMatchingResources(Object target, @Type(clazz = ResourceManagerProxy.class) Object resourceManager);
}