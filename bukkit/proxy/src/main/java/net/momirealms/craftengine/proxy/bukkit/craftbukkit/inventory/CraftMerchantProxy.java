package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftMerchant")
public interface CraftMerchantProxy {
    CraftMerchantProxy INSTANCE = ASMProxyFactory.create(CraftMerchantProxy.class);

    @MethodInvoker(name = "getMerchant")
    Object getMerchant(Object target);
}
