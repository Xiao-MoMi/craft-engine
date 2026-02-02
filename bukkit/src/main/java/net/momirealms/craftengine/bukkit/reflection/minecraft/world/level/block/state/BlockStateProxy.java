package net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.block.state;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockState")
public interface BlockStateProxy {
    BlockStateProxy INSTANCE = ReflectionHelper.getProxy(BlockStateProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BlockStateProxy.class);
}
