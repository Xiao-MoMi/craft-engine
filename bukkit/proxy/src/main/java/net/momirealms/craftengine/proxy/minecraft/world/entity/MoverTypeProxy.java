package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.MoverType")
public interface MoverTypeProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.MoverType");
}
