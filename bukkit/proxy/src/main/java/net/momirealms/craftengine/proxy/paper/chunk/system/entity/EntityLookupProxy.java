package net.momirealms.craftengine.proxy.paper.chunk.system.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"io.papermc.paper.chunk.system.entity.EntityLookup", "ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup"})
public interface EntityLookupProxy {
    EntityLookupProxy INSTANCE = ASMProxyFactory.create(EntityLookupProxy.class);

    @FieldGetter(name = "worldCallback")
    Object getWorldCallback(Object target);

    @FieldSetter(name = "worldCallback")
    void setWorldCallback(Object target, Object value);

    @MethodInvoker(name = "get")
    Object get(Object target, int id);
}
