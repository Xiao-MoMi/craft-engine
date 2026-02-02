package net.momirealms.craftengine.bukkit.reflection.craftbukkit;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftChunk")
public interface CraftChunkProxy {
    CraftChunkProxy INSTANCE = ReflectionHelper.getProxy(CraftChunkProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftChunkProxy.class);

    @FieldGetter(names = {"level", "worldServer"})
    Object level(Object instance);

    @FieldSetter(names = {"level", "worldServer"}, strategy = Strategy.MH)
    void level(Object instance, Object level);
}
