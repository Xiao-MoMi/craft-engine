package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.DyeItem")
public interface DyeItemProxy {
    DyeItemProxy INSTANCE = ASMProxyFactory.create(DyeItemProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.DyeItem");

    @FieldGetter(name = "dyeColor")
    Object getDyeColor(Object target);
}
