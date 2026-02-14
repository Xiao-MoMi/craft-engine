package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.Items")
public interface ItemsProxy {
    ItemsProxy INSTANCE = ASMProxyFactory.create(ItemsProxy.class);
    Object AIR = INSTANCE.getAir();
    Object WATER_BUCKET = INSTANCE.getWaterBucket();
    Object BARRIER = INSTANCE.getBarrier();
    Object DEBUG_STICK = INSTANCE.getDebugStick();

    @FieldGetter(name = "AIR", isStatic = true)
    Object getAir();

    @FieldGetter(name = "WATER_BUCKET", isStatic = true)
    Object getWaterBucket();

    @FieldGetter(name = "BARRIER", isStatic = true)
    Object getBarrier();

    @FieldGetter(name = "DEBUG_STICK", isStatic = true)
    Object getDebugStick();
}
