package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.Display")
public interface DisplayProxy {
    @ReflectionProxy(name = "net.minecraft.world.entity.Display$ItemDisplay")
    interface ItemDisplayProxy {
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.Display$ItemDisplay");
    }
}
