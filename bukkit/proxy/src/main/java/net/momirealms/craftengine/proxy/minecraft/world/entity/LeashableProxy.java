package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.Leashable", activeIf = "min_version=1.21")
public interface LeashableProxy {
    @ReflectionProxy(name = "net.minecraft.world.entity.Leashable$LeashData", activeIf = "min_version=1.21")
    interface LeashDataProxy {
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.Leashable$LeashData");
    }
}
