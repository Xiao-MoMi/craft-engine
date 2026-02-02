package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.Inventory;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftContainer")
public interface CraftContainerProxy {
    CraftContainerProxy INSTANCE = ReflectionHelper.getProxy(CraftContainerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftContainerProxy.class);

    @ConstructorInvoker
    Object newInstance(Inventory inventory, @Type(clazz = PlayerProxy.class) Object player, int id);

    @MethodInvoker(name = "getNotchInventoryType", isStatic = true)
    Object getNotchInventoryType(Inventory inventory);
}
