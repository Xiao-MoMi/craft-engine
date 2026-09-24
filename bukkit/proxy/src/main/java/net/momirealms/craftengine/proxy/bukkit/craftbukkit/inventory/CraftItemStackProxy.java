package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.ItemStack;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftItemStack")
public interface CraftItemStackProxy {
    CraftItemStackProxy INSTANCE = ASMProxyFactory.create(CraftItemStackProxy.class);
    Class<?> CLASS = SparrowClass.find("org.bukkit.craftbukkit.inventory.CraftItemStack");

    @FieldGetter(name = "handle")
    Object getHandle(ItemStack target);

    @MethodInvoker(name = "asCraftCopy", isStatic = true, activeIf = "max_version=26.2")
    default ItemStack asCraftCopy(ItemStack original) {
        return asCraftMirror(asNMSCopy(original));
    }

    @MethodInvoker(name = "asNMSCopy", isStatic = true)
    Object asNMSCopy(ItemStack original);

    @MethodInvoker(name = {"asBukkitMirror", "asCraftMirror"}, isStatic = true)
    ItemStack asCraftMirror(@Type(clazz = ItemStackProxy.class) Object original);
}
