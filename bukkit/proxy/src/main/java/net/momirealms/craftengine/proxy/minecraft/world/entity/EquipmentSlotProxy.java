package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.EquipmentSlot")
public interface EquipmentSlotProxy {
    EquipmentSlotProxy INSTANCE = ASMProxyFactory.create(EquipmentSlotProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> MAINHAND = VALUES[0];
    Enum<?> OFFHAND = VALUES[1];
    Enum<?> FEET = VALUES[2];
    Enum<?> LEGS = VALUES[3];
    Enum<?> CHEST = VALUES[4];
    Enum<?> HEAD = VALUES[5];
    Enum<?> BODY = VALUES.length > 6 ? VALUES[6] : null;
    Enum<?> SADDLE = VALUES.length > 7 ? VALUES[7] : null;

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
