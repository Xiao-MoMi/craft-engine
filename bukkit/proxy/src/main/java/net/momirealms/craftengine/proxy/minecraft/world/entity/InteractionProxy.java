package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.Interaction")
public interface InteractionProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.Interaction");
}
