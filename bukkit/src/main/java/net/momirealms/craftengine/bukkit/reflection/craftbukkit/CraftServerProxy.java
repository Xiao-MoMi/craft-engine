package net.momirealms.craftengine.bukkit.reflection.craftbukkit;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.Server;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftServer")
public interface CraftServerProxy {
    CraftServerProxy INSTANCE = ReflectionHelper.getProxy(CraftServerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftServerProxy.class);

    @FieldGetter(name = "playerList")
    Object playerList(Server instance);

    @FieldSetter(name = "playerList", strategy = Strategy.MH)
    void playerList(Server instance, Object playerList);
}
