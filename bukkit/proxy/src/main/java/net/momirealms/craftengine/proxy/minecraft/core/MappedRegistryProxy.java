package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.core.MappedRegistry")
public interface MappedRegistryProxy {
    MappedRegistryProxy INSTANCE = ASMProxyFactory.create(MappedRegistryProxy.class);

    @FieldGetter(name = "frozen")
    boolean isFrozen(Object target);

    @FieldSetter(name = "frozen")
    void setFrozen(Object target, boolean frozen);

    @FieldSetter(name = "unregisteredIntrusiveHolders")
    void setUnregisteredIntrusiveHolders(Object target, Map<Object, Object> intrusiveHolders);
}
