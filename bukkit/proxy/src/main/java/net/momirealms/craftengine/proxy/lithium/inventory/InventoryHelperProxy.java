package net.momirealms.craftengine.proxy.lithium.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.caffeinemc.mods.lithium.common.hopper.InventoryHelper", optional = true)
public interface InventoryHelperProxy {
    InventoryHelperProxy INSTANCE = ASMProxyFactory.create(InventoryHelperProxy.class);
    Class<?> CLASS = SparrowClass.find("net.caffeinemc.mods.lithium.common.hopper.InventoryHelper");

    @MethodInvoker(name = "getLithiumStackList", isStatic = true)
    Object getLithiumStackList(@Type(clazz = LithiumInventoryProxy.class) Object inventory);
}
