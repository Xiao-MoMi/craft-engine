package net.momirealms.craftengine.proxy.bukkit.craftbukkit;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.World;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftWorld")
public interface CraftWorldProxy {
    CraftWorldProxy INSTANCE = ASMProxyFactory.create(CraftWorldProxy.class);

    @FieldGetter(name = "world")
    Object getWorld(World target);

    @FieldSetter(name = "world")
    void setWorld(World target, Object world);
}
