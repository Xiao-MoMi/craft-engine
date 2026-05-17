package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.WorldGenRegion")
public interface WorldGenRegionProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.server.level.WorldGenRegion");
}
