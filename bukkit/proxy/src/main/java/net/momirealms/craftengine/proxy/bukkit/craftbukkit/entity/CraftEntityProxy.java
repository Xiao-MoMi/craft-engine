package net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.entity.Entity;

@ReflectionProxy(name = "org.bukkit.craftbukkit.entity.CraftEntity")
public interface CraftEntityProxy {
    CraftEntityProxy INSTANCE = ASMProxyFactory.create(CraftEntityProxy.class);

    @FieldGetter(name = "entity")
    Object getEntity(Entity target);

    @FieldSetter(name = "entity")
    void setEntity(Entity target, Object entity);
}
