package net.momirealms.craftengine.bukkit.reflection.minecraft.world.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.ChunkPos")
public interface ChunkPosProxy {
    ChunkPosProxy INSTANCE = ReflectionHelper.getProxy(ChunkPosProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ChunkPosProxy.class);
}
