package net.momirealms.craftengine.bukkit.reflection.minecraft.core;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.HolderLookup")
public interface HolderLookupProxy {
    HolderLookupProxy INSTANCE = ReflectionHelper.getProxy(HolderLookupProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(HolderLookupProxy.class);

    @ReflectionProxy(name = "net.minecraft.core.HolderLookup$RegistryLookup")
    interface RegistryLookupProxy {
        RegistryLookupProxy INSTANCE = ReflectionHelper.getProxy(RegistryLookupProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(RegistryLookupProxy.class);
    }
}
