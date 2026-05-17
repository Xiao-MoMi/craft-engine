package net.momirealms.craftengine.proxy.minecraft.world.entity.item;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.world.entity.item.ItemEntity")
public interface ItemEntityProxy {
    ItemEntityProxy INSTANCE = ASMProxyFactory.create(ItemEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.item.ItemEntity");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = LevelProxy.class) Object world, double x, double y, double z, @Type(clazz = ItemStackProxy.class) Object stack);

    @FieldGetter(name = "target")
    UUID getTarget(Object target);

    @FieldSetter(name = "target")
    void setTarget$0(Object target$0, UUID target$1);

    @FieldGetter(name = "pickupDelay")
    int getPickupDelay(Object target);

    @FieldSetter(name = "pickupDelay")
    void setPickupDelay(Object target, int pickupDelay);

    @FieldGetter(name = "age")
    int getAge(Object target);

    @FieldSetter(name = "age")
    void setAge(Object target, int age);

    @FieldGetter(name = "despawnRate")
    int getDespawnRate(Object target);

    @FieldSetter(name = "despawnRate")
    void setDespawnRate(Object target, int despawnRate);

    @MethodInvoker(name = "setNoPickUpDelay")
    void setNoPickUpDelay(Object target);

    @MethodInvoker(name = "makeFakeItem")
    void makeFakeItem(Object target);

    @MethodInvoker(name = "setTarget")
    void setTarget$1(Object target0, UUID target$1);

    @MethodInvoker(name = "setDefaultPickUpDelay")
    void setDefaultPickUpDelay(Object target);

    @MethodInvoker(name = "getItem")
    Object getItem(Object target);

    @MethodInvoker(name = "setItem")
    void setItem(Object target, @Type(clazz = ItemStackProxy.class) Object item);
}
