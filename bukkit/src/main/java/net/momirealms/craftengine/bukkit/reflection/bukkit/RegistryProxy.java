package net.momirealms.craftengine.bukkit.reflection.bukkit;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@ReflectionProxy(clazz = Registry.class)
public interface RegistryProxy {
    RegistryProxy INSTANCE = ReflectionHelper.getProxy(RegistryProxy.class);
    Class<?> CLAZZ = Registry.class;

    @SuppressWarnings("UnstableApiUsage")
    @ReflectionProxy(clazz = Registry.SimpleRegistry.class)
    interface SimpleRegistryProxy {
        SimpleRegistryProxy INSTANCE = ReflectionHelper.getProxy(SimpleRegistryProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(SimpleRegistryProxy.class);

        @FieldGetter(name = "map")
        <T extends Enum<T> & Keyed> Map<NamespacedKey, T> map(Registry.SimpleRegistry<@NotNull T> instance);

        @FieldSetter(name = "map", strategy = Strategy.MH)
        <T extends Enum<T> & Keyed> void map(Registry.SimpleRegistry<@NotNull T> instance, Map<NamespacedKey, T> map);
    }
}
