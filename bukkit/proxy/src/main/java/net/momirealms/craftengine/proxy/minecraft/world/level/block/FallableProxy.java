package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.Fallable")
public interface FallableProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.Fallable");
}
