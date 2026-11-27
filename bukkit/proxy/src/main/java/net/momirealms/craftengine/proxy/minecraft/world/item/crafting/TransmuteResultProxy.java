package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.TransmuteResult", activeIf = "min_version=1.21.5 && max_version=1.21.11")
public interface TransmuteResultProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.crafting.TransmuteResult");
}
