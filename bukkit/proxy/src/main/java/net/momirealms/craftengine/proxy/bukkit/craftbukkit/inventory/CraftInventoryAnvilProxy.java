package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftInventoryAnvil")
public interface CraftInventoryAnvilProxy {
    CraftInventoryAnvilProxy INSTANCE = ASMProxyFactory.create(CraftInventoryAnvilProxy.class);

    @FieldGetter(name = "container", activeIf = "max_version=1.20.6")
    Object getContainer(Object target);
}
