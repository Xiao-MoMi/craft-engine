package net.momirealms.craftengine.proxy.minecraft.world.level.block.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity")
public interface AbstractFurnaceBlockEntityProxy extends BaseContainerBlockEntityProxy {
    AbstractFurnaceBlockEntityProxy INSTANCE = ASMProxyFactory.create(AbstractFurnaceBlockEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity");

    @FieldGetter(name = "quickCheck")
    Object getQuickCheck(Object target);

    @FieldSetter(name = "quickCheck")
    void setQuickCheck(Object target, Object value);

    @MethodInvoker(name = "getItem", activeIf = "max_version=1.20.4")
    Object getItem(Object target, int slot);

    @FieldGetter(name = "items")
    List<Object> getItems(Object target);

    @FieldGetter(name = {"litTimeRemaining", "litTime"})
    int getLitTimeRemaining(Object target);
}
