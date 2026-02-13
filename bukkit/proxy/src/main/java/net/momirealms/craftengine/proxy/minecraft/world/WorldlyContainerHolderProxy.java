package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.WorldlyContainerHolder")
public interface WorldlyContainerHolderProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.WorldlyContainerHolder");
}
