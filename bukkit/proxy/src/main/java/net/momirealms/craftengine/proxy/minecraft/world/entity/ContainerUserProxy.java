package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.ContainerUser", activeIf = "min_version=1.21.9")
public interface ContainerUserProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.ContainerUser");
}
