package net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.entity.CraftEntity")
public interface CraftEntityProxy {
    CraftEntityProxy INSTANCE = ASMProxyFactory.create(CraftEntityProxy.class);

    @FieldGetter(name = "entity")
    Object getEntity(Object target);

    @FieldSetter(name = "entity")
    void setEntity(Object target, Object entity);
}
