package net.momirealms.craftengine.bukkit.reflection.minecraft.world.item;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.ItemStack")
public interface ItemStackProxy {
    ItemStackProxy INSTANCE = ReflectionHelper.getProxy(ItemStackProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ItemStackProxy.class);
}
