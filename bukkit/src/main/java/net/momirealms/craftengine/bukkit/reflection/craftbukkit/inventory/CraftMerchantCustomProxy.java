package net.momirealms.craftengine.bukkit.reflection.craftbukkit.inventory;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftMerchantCustom")
public interface CraftMerchantCustomProxy {
    CraftMerchantCustomProxy INSTANCE = ReflectionHelper.getProxy(CraftMerchantCustomProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(CraftMerchantCustomProxy.class);

    @ReflectionProxy(name = "org.bukkit.craftbukkit.inventory.CraftMerchantCustom$MinecraftMerchant")
    interface MinecraftMerchantProxy {
        MinecraftMerchantProxy INSTANCE = ReflectionHelper.getProxy(MinecraftMerchantProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(MinecraftMerchantProxy.class);

        @FieldGetter(name = "title")
        Object title(Object instance);

        @FieldSetter(name = "title", strategy = Strategy.MH)
        void title(Object instance, Object title);
    }
}
