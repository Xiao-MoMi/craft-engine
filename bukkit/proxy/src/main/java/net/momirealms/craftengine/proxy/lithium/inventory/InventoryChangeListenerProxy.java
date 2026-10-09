package net.momirealms.craftengine.proxy.lithium.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeListener", optional = true)
public interface InventoryChangeListenerProxy {
    Class<?> CLASS = SparrowClass.find("net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeListener");
}
