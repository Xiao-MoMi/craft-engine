package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryView", activeIf = "min_version=1.21")
public interface CraftInventoryViewProxy {
    CraftInventoryViewProxy INSTANCE = ASMProxyFactory.create(CraftInventoryViewProxy.class);

    @FieldGetter(name = "container")
    Object getContainer(Object target);
}
