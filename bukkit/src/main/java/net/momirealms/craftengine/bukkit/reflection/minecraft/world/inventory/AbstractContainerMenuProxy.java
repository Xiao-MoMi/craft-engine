package net.momirealms.craftengine.bukkit.reflection.minecraft.world.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.inventory.AbstractContainerMenu")
public interface AbstractContainerMenuProxy {
    AbstractContainerMenuProxy INSTANCE = ReflectionHelper.getProxy(AbstractContainerMenuProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(AbstractContainerMenuProxy.class);
}
