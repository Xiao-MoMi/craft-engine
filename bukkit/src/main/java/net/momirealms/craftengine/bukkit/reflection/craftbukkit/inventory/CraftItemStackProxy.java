package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.ItemStack;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftItemStack")
public interface CraftItemStackProxy {
    CraftItemStackProxy INSTANCE = ReflectionHelper.getProxy(CraftItemStackProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftItemStackProxy.class);

    @MethodInvoker(name = "asCraftMirror", isStatic = true)
    ItemStack asCraftMirror(@Type(clazz = ItemStackProxy.class) Object itemStack);
}
