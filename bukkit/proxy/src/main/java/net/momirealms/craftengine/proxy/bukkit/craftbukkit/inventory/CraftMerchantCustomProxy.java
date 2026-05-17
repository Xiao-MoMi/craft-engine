package net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftMerchantCustom")
public interface CraftMerchantCustomProxy {
    CraftMerchantCustomProxy INSTANCE = ASMProxyFactory.create(CraftMerchantCustomProxy.class);

    @ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftMerchantCustom$MinecraftMerchant")
    interface MinecraftMerchantProxy {
        MinecraftMerchantProxy INSTANCE = ASMProxyFactory.create(MinecraftMerchantProxy.class);

        @FieldGetter(name = "title")
        Object getTitle(Object target);

        @FieldSetter(name = "title")
        void setTitle(Object target, Object title);
    }
}
