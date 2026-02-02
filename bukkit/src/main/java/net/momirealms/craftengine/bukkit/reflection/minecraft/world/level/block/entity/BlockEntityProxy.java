package net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.block.entity;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.BlockEntity")
public interface BlockEntityProxy {
    BlockEntityProxy INSTANCE = ReflectionHelper.getProxy(BlockEntityProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BlockEntityProxy.class);
}
