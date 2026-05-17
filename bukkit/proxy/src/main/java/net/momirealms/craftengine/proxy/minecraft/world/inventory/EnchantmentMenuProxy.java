package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.inventory.EnchantmentMenu")
public interface EnchantmentMenuProxy extends AbstractContainerMenuProxy {
    EnchantmentMenuProxy INSTANCE = ASMProxyFactory.create(EnchantmentMenuProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.inventory.EnchantmentMenu");
}
