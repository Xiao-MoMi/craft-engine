package net.momirealms.craftengine.proxy.lithium.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.caffeinemc.mods.lithium.api.inventory.LithiumInventory", optional = true)
public interface LithiumInventoryProxy {
    LithiumInventoryProxy INSTANCE = ASMProxyFactory.create(LithiumInventoryProxy.class);
    Class<?> CLASS = SparrowClass.find("net.caffeinemc.mods.lithium.api.inventory.LithiumInventory");

    @MethodInvoker(name = "getInventoryLithium")
    Object getInventoryLithium(Object target);
}
