package net.momirealms.craftengine.proxy.minecraft.world.entity.item;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.world.entity.item.ItemEntity")
public interface ItemEntityProxy {
    ItemEntityProxy INSTANCE = ASMProxyFactory.create(ItemEntityProxy.class);

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
}
