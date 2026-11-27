package net.momirealms.craftengine.proxy.minecraft.core.dispenser;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.dispenser.DispenseItemBehavior")
public interface DispenseItemBehaviorProxy {
    DispenseItemBehaviorProxy INSTANCE = ASMProxyFactory.create(DispenseItemBehaviorProxy.class);
    Object NOOP = INSTANCE.noop();

    @FieldGetter(name = "NOOP", isStatic = true)
    Object noop();

    @MethodInvoker(name = "dispense")
    Object dispense(Object target, @Type(clazz = BlockSourceProxy.class) Object source, @Type(clazz = ItemStackProxy.class) Object stack);
}
