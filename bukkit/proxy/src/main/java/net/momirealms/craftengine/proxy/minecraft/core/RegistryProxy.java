package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.core.Registry")
public interface RegistryProxy extends IdMapProxy, HolderLookupProxy {
    RegistryProxy INSTANCE = ASMProxyFactory.create(RegistryProxy.class);

    @MethodInvoker(name = "registerForHolder", isStatic = true)
    Object registerForHolder$0(@Type(clazz = RegistryProxy.class) Object registry,
                               @Type(clazz = ResourceKeyProxy.class) Object resourceKey,
                               Object value);

    @MethodInvoker(name = "registerForHolder", isStatic = true)
    Object registerForHolder$1(@Type(clazz = RegistryProxy.class) Object registry,
                               @Type(clazz = IdentifierProxy.class) Object identifier,
                               Object value);

    @MethodInvoker(name = "asLookup", activeIf = "max_version=1.21.1")
    Object asLookup(Object target);

    @MethodInvoker(name = "get", activeIf = "min_version=1.21.2")
    Optional<Object> get$0(Object target, @Type(clazz = IdentifierProxy.class) Object id);

    @MethodInvoker(name = "getHolder", activeIf = "min_version=1.20.5 && max_version=1.21.1")
    Optional<Object> getHolder$0(Object target, @Type(clazz = IdentifierProxy.class) Object id);

    @MethodInvoker(name = "getId")
    int getId$0(Object target, Object value);

    @MethodInvoker(name = "getKey")
    Object getKey(Object target, Object value);

    @MethodInvoker(name = "getValue", activeIf = "min_version=1.21.2")
    Object getValue(Object target, @Type(clazz = IdentifierProxy.class) Object id);

    @MethodInvoker(name = "get", activeIf = "max_version=1.21.1")
    Object get$2(Object target, @Type(clazz = IdentifierProxy.class) Object id);

    @MethodInvoker(name = "getHolder", activeIf = "max_version=1.21.1")
    Optional<Object> getHolder$1(Object target, @Type(clazz = ResourceKeyProxy.class) Object key);
}
