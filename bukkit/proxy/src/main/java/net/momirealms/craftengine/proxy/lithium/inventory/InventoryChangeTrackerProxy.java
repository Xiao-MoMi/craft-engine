package net.momirealms.craftengine.proxy.lithium.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker", optional = true)
public interface InventoryChangeTrackerProxy {
    InventoryChangeTrackerProxy INSTANCE = ASMProxyFactory.create(InventoryChangeTrackerProxy.class);
    Class<?> CLASS = SparrowClass.find("net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker");

    @MethodInvoker(name = "listenForContentChangesOnce")
    void listenForContentChangesOnce(Object target, @Type(clazz = LithiumStackListProxy.class) Object stackList,
                                    @Type(clazz = InventoryChangeListenerProxy.class) Object listener);

    @MethodInvoker(name = "listenForMajorInventoryChanges")
    void listenForMajorInventoryChanges(Object target, @Type(clazz = InventoryChangeListenerProxy.class) Object listener);

    @MethodInvoker(name = "stopListenForMajorInventoryChanges")
    void stopListenForMajorInventoryChanges(Object target, @Type(clazz = InventoryChangeListenerProxy.class) Object listener);
}
