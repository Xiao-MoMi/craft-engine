package net.momirealms.craftengine.bukkit.reflection.paper.chunk.system.entity;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.jetbrains.annotations.Nullable;

@ReflectionProxy(names = {
        "ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup",
        "io.papermc.paper.chunk.system.entity.EntityLookup"
})
public interface EntityLookupProxy {
    EntityLookupProxy INSTANCE = ReflectionHelper.getProxy(EntityLookupProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(EntityLookupProxy.class);

    @MethodInvoker(name = "get")
    @Nullable Object get(Object instance, int id);

    @FieldGetter(name = "worldCallback")
    Object worldCallback(Object instance);

    @FieldSetter(name = "worldCallback", strategy = Strategy.MH)
    void worldCallback(Object instance, Object worldCallback);
}
