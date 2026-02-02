package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.Merchant;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftMerchant")
public interface CraftMerchantProxy {
    CraftMerchantProxy INSTANCE = ReflectionHelper.getProxy(CraftMerchantProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftMerchantProxy.class);

    @MethodInvoker(name = "getMerchant")
    Object getMerchant(Merchant instance);
}
