package net.momirealms.craftengine.proxy.minecraft.core.dispenser;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.dispenser.DefaultDispenseItemBehavior")
public interface DefaultDispenseItemBehaviorProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.dispenser.DefaultDispenseItemBehavior");
}
