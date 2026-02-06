package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.Inventory;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftContainer")
public interface CraftContainerProxy extends AbstractContainerMenuProxy {
    CraftContainerProxy INSTANCE = ASMProxyFactory.create(CraftContainerProxy.class);

    @MethodInvoker(name = "getNotchInventoryType")
    Object getNotchInventoryType(Inventory inventory);

    Object newInstance(Inventory inventory,
                       @Type(clazz = PlayerProxy.class) Object player,
                       int id);
}
