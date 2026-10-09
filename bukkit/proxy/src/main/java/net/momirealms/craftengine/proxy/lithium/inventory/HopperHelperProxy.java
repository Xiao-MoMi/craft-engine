package net.momirealms.craftengine.proxy.lithium.inventory;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.caffeinemc.mods.lithium.common.hopper.HopperHelper", optional = true)
public interface HopperHelperProxy {
    HopperHelperProxy INSTANCE = ASMProxyFactory.create(HopperHelperProxy.class);
    Class<?> CLASS = SparrowClass.find("net.caffeinemc.mods.lithium.common.hopper.HopperHelper");

    @MethodInvoker(name = "replaceDoubleInventory", isStatic = true)
    Object replaceDoubleInventory(@Type(name = "net.minecraft.world.Container") Object inventory);
}
