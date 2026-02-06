package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.EquipmentSlot")
public interface EquipmentSlotProxy {
    EquipmentSlotProxy INSTANCE = ASMProxyFactory.create(EquipmentSlotProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object MAINHAND = VALUES[0];
    Object OFFHAND = VALUES[1];
    Object FEET = VALUES[2];
    Object LEGS = VALUES[3];
    Object CHEST = VALUES[4];
    Object HEAD = VALUES[5];
    Object BODY = VALUES.length > 6 ? VALUES[6] : null;
    Object SADDLE = VALUES.length > 7 ? VALUES[7] : null;

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
