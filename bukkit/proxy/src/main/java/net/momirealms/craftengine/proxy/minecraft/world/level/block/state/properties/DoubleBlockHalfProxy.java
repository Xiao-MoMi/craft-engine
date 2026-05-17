package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.properties.DoubleBlockHalf")
public interface DoubleBlockHalfProxy {
    @SuppressWarnings("unchecked")
    Class<? extends Enum<?>> CLASS = (Class<? extends Enum<?>>) SparrowClass.find("net.minecraft.world.level.block.state.properties.DoubleBlockHalf");
}
