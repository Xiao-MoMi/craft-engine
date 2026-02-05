package net.momirealms.craftengine.proxy.resource;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.resources.ResourceKey")
public interface ResourceKeyProxy {
    ResourceKeyProxy INSTANCE = ASMProxyFactory.create(ResourceKeyProxy.class);

    @FieldGetter(name = "registryName")
    Object getRegistryName(Object target);

    @FieldSetter(name = "registryName")
    void setRegistryName(Object target, Object registryName);

    @FieldGetter(name = {"location", "identifier"})
    Object getLocation(Object target);

    @FieldSetter(name = {"location", "identifier"})
    void setLocation(Object target, Object location);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(@Type(clazz = ResourceKeyProxy.class) Object registry, @Type(clazz = IdentifierProxy.class) Object value);
}