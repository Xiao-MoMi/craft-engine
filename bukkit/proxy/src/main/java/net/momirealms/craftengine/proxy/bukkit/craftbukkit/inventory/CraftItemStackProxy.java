package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.ItemStack;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftItemStack")
public interface CraftItemStackProxy {
    CraftItemStackProxy INSTANCE = ASMProxyFactory.create(CraftItemStackProxy.class);
    Class<?> CLASS = SparrowClass.find("org.bukkit.craftbukkit.inventory.CraftItemStack");

    @MethodInvoker(name = "asCraftCopy", isStatic = true)
    ItemStack asCraftCopy(ItemStack original);

    @MethodInvoker(name = "asNMSCopy", isStatic = true)
    Object asNMSCopy(ItemStack original);

    @MethodInvoker(name = "unwrap", isStatic = true)
    Object unwrap(ItemStack bukkit);

    @MethodInvoker(name = "asCraftMirror", isStatic = true)
    ItemStack asCraftMirror(@Type(clazz = ItemStackProxy.class) Object original);
}
