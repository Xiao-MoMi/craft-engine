package net.momirealms.craftengine.proxy.minecraft.world.item.trading;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.trading.MerchantOffer")
public interface MerchantOfferProxy {
    MerchantOfferProxy INSTANCE = ASMProxyFactory.create(MerchantOfferProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.trading.MerchantOffer");

    @MethodInvoker(name = "getCostA")
    Object getCostA(Object target);

    @MethodInvoker(name = "getCostB")
    Object getCostB(Object target);
}
