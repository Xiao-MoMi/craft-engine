package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.CrossbowItem")
public interface CrossbowItemProxy {
    CrossbowItemProxy INSTANCE = ASMProxyFactory.create(CrossbowItemProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.CrossbowItem");

    @MethodInvoker(name = "getChargeDuration", isStatic = true, activeIf = "max_version=1.20.6")
    int getChargeDuration(@Type(clazz = ItemStackProxy.class) Object stack);

    @MethodInvoker(name = "getChargeDuration", isStatic = true, activeIf = "min_version=1.21")
    int getChargeDuration(@Type(clazz = ItemStackProxy.class) Object stack, @Type(clazz = LivingEntityProxy.class) Object entity);
}
